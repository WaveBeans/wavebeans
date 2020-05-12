package io.wavebeans.execution.distributed

import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.wavebeans.execution.*
import io.wavebeans.execution.config.ExecutionConfig
import io.wavebeans.execution.medium.MediumBuilder
import io.wavebeans.execution.medium.PodCallResultBuilder
import io.wavebeans.execution.pod.PodKey
import io.wavebeans.lib.WaveBeansClassLoader
import mu.KotlinLogging
import java.io.Closeable
import java.io.File
import java.io.OutputStream
import java.net.InetAddress
import java.util.concurrent.*
import java.util.jar.JarFile
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.forEach
import kotlin.collections.joinToString
import kotlin.collections.map
import kotlin.collections.set
import kotlin.collections.toList
import kotlin.random.Random
import kotlin.random.nextInt

class Facilitator(
        private val advertisingHostAddress: String,
        private val listeningPortRange: IntRange,
        private val startingUpAttemptsCount: Int,
        private val threadsNumber: Int,
        private val applicationEngine: ApplicationEngine? = null,
        private val gardener: Gardener = Gardener(),
        private val callTimeoutMillis: Long = 5000L,
        private val onServerShutdownGracePeriodMillis: Long = 5000L,
        private val onServerShutdownTimeoutMillis: Long = 5000L,
        private val podCallResultBuilder: PodCallResultBuilder = SerializablePodCallResultBuilder(),
        private val mediumBuilder: MediumBuilder = SerializableMediumBuilder(),
        private val executionThreadPool: ExecutionThreadPool = MultiThreadedExecutionThreadPool(threadsNumber),
        private val podDiscovery: PodDiscovery = PodDiscovery.default
) : Closeable {

    companion object {
        private val log = KotlinLogging.logger { }
        private val jvmFacilitators = ConcurrentHashMap<Int, Facilitator>()
    }

    val tempDir = createTempDir("wavebeans-facilitator")

    private val terminate = CountDownLatch(1)
    private val startupClasses = startUpClasses()

    class JobState(
            val jobKey: JobKey,
            val classLoader: FacilitatorClassLoader,
            val futures: MutableList<Future<ExecutionResult>>,
            val remoteBushes: MutableList<RemoteBush>
    ) {
        companion object {
            fun create(jobKey: JobKey): JobState {
                val cl = FacilitatorClassLoader(FacilitatorClassLoader(this::class.java.classLoader))
                WaveBeansClassLoader.addClassLoader(cl)
                return JobState(
                        jobKey,
                        cl,
                        CopyOnWriteArrayList(),
                        CopyOnWriteArrayList()
                )
            }
        }
    }

    private lateinit var server: ApplicationEngine
    private var listeningPort: Int = 0
    private var startedFrom: List<StackTraceElement>? = null
    private val jobStates = ConcurrentHashMap<JobKey, JobState>()

    fun start(): Facilitator {
        if (startedFrom != null)
            throw IllegalStateException("Facilitator $this is already started from: " +
                    startedFrom!!.joinToString("\n") { "\tat $it" })
        startedFrom = Thread.currentThread().stackTrace.toList()
        log.info {
            "Advertising on host:\n" +
                    InetAddress.getAllByName(advertisingHostAddress).joinToString("\n") {
                        """
                        \tcanonicalHostName = ${it.canonicalHostName}
                        \thostAddress = ${it.hostAddress}
                        \thostName = ${it.hostName}
                    """.trimIndent()
                    }
        }

        ExecutionConfig.podCallResultBuilder(podCallResultBuilder)
        ExecutionConfig.mediumBuilder(mediumBuilder)
        ExecutionConfig.executionThreadPool(executionThreadPool)

        listeningPort = Random.nextInt(listeningPortRange)
        var attempt = startingUpAttemptsCount
        while (attempt > 0) {
            try {
                startApi(listeningPort)
                attempt = -1
            } catch (e: java.net.BindException) {
                attempt--
                log.debug(e) {
                    "Can't start server on port $listeningPort. Choosing random port within range " +
                            "$listeningPortRange. Left $attempt attempts before quit.\n" +
                            "Another started facilitator on the same JVM:\n" +
                            jvmFacilitators.entries.joinToString("\n\n") {
                                "on ${it.key}: ${it.value}" +
                                        "started from:\n " + it.value.startedFrom?.joinToString("\n") { "\tat $it" }
                            }
                }
                listeningPort = Random.nextInt(listeningPortRange)
            }
        }
        if (attempt == 0) throw IllegalStateException("Can't start the Facilitator within $startingUpAttemptsCount attempts.")

        log.info { "Facilitator API started on port $listeningPort" }
        jvmFacilitators[listeningPort] = this
        return this
    }

    fun listeningPort(): Int =
            if (listeningPort > 0) listeningPort
            else throw IllegalStateException("Facilitator is not listening yet. Please start first")

    fun startupClasses(): List<ClassDesc> = startupClasses

    fun registerCode(jobKey: JobKey, codeFile: File) {
        log.info {
            "[JobKey $jobKey] Registering code from jar $codeFile. Code file content:\n" +
                    JarFile(codeFile).entries().asSequence().joinToString("\n") { "${it.name} size=${it.size}bytes crc=${it.crc}" }
        }

        jobStates(jobKey).classLoader += codeFile.toURI().toURL()
    }

    fun plantBush(request: PlantBushRequest) {
        gardener.plantBush(request.jobKey, request.jobContent.bushKey, request.jobContent.pods, request.sampleRate)
        jobStates(request.jobKey).futures.addAll(gardener.getAllFutures(request.jobKey))
    }

    fun registerBushEndpoints(registerBushEndpointsRequest: RegisterBushEndpointsRequest) {
        val remoteBushes = jobStates(registerBushEndpointsRequest.jobKey).remoteBushes
        registerBushEndpointsRequest.bushEndpoints.forEach { bushEndpoint ->
            val bushKey = bushEndpoint.bushKey
            val remoteBush = RemoteBush(bushKey, bushEndpoint.location)
            podDiscovery.registerBush(bushKey, remoteBush)
            remoteBushes.add(remoteBush)
            bushEndpoint.pods.forEach { podDiscovery.registerPod(bushKey, it) }
        }
    }

    fun startJob(jobKey: JobKey) {
        gardener.start(jobKey)
    }

    fun jobs(): List<JobKey> {
        return gardener.jobs()
    }

    fun describeJob(jobKey: JobKey): List<JobContent> =
            gardener.job(jobKey).map { JobContent(it.bushKey, it.podRefs) }

    fun status(jobKey: JobKey): List<JobStatus> {
        return gardener.getAllFutures(jobKey).map { future ->
            when {
                future.isDone -> {
                    try {
                        val result = future.get(5000, TimeUnit.MILLISECONDS)
                        JobStatus(
                                jobKey,
                                if (result.exception == null) FutureStatus.DONE else FutureStatus.FAILED,
                                result.exception?.let { ExceptionObj.create(it) }
                        )
                    } catch (e: ExecutionException) {
                        JobStatus(jobKey, FutureStatus.FAILED, ExceptionObj.create(e.cause ?: e))
                    }
                }
                future.isCancelled -> JobStatus(jobKey, FutureStatus.CANCELLED, null)
                else -> JobStatus(jobKey, FutureStatus.IN_PROGRESS, null)
            }
        }
    }

    fun call(bushKey: BushKey, podKey: PodKey, request: String): (OutputStream) -> Unit {
        val bush = podDiscovery.bush(bushKey) ?: throw IllegalStateException("Bush $bushKey not found")
        if (bush !is LocalBush) throw IllegalStateException("Bush $bushKey is ${bush::class} but ${LocalBush::class} expected")
        val podCallResult = bush.call(podKey, request).get(callTimeoutMillis, TimeUnit.MILLISECONDS)
        return podCallResult::writeTo
    }

    fun stopJob(jobKey: JobKey) {
        gardener.stop(jobKey)
        jobStates.remove(jobKey)?.let { jobState ->
            jobState.remoteBushes.forEach { bush ->
                bush.close()
                podDiscovery.unregisterBush(bush.bushKey)
            }
            WaveBeansClassLoader.removeClassLoader(jobState.classLoader)
        }
    }

    fun stopAll() {
        jobStates.keys.forEach { stopJob(it) }
    }

    fun waitAndClose() {
        terminate.await()
        log.info { "Facilitator terminating" }
        close()
        log.info { "Facilitator stopped" }
    }

    fun terminate() {
        log.info { "Terminating" }
        gardener.stopAll()
        terminate.countDown()
        stopAll()
    }

    override fun close() {
        if (applicationEngine == null)
            server.stop(onServerShutdownGracePeriodMillis, onServerShutdownTimeoutMillis)
    }

    private fun startApi(serverPort: Int) {
        if (applicationEngine == null) {
            val env = applicationEngineEnvironment {
                module {
                    facilitatorApi(this@Facilitator)
                }
                connector {
                    host = "0.0.0.0"
                    port = serverPort
                }
            }
            server = embeddedServer(Netty, env).start()
        } else {
            applicationEngine.application.facilitatorApi(this)
        }
    }

    private fun jobStates(jobKey: JobKey): JobState {
        return jobStates.computeIfAbsent(jobKey) { JobState.create(jobKey) }
    }
}


