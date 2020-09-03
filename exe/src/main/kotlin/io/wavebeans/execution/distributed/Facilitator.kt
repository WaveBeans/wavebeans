package io.wavebeans.execution.distributed

import io.grpc.ServerBuilder
import io.wavebeans.communicator.JobStatusResponse
import io.wavebeans.communicator.JobStatusResponse.JobStatus.FutureStatus.*
import io.wavebeans.execution.*
import io.wavebeans.execution.config.ExecutionConfig
import io.wavebeans.execution.medium.MediumBuilder
import io.wavebeans.execution.medium.PodCallResultBuilder
import io.wavebeans.execution.pod.PodKey
import io.wavebeans.lib.WaveBeansClassLoader
import io.wavebeans.lib.table.TableRegistry
import io.wavebeans.metrics.collector.MetricGrpcService
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.modules.SerializersModule
import mu.KotlinLogging
import java.io.Closeable
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.*

data class BushEndpoint(
        val bushKey: BushKey,
        val location: String,
        val pods: List<PodKey>
)

class Facilitator(
        private val threadsNumber: Int,
        private val communicatorPort: Int? = null,
        private val gardener: Gardener = Gardener(),
        private val callTimeoutMillis: Long = 5000L,
        private val onServerShutdownTimeoutMillis: Long = 5000L,
        private val podCallResultBuilder: PodCallResultBuilder = SerializablePodCallResultBuilder(),
        private val mediumBuilder: MediumBuilder = SerializableMediumBuilder(),
        private val executionThreadPool: ExecutionThreadPool = MultiThreadedExecutionThreadPool(threadsNumber),
        private val podDiscovery: PodDiscovery = PodDiscovery.default
        // TODO probably inject table registry also
) : Closeable {

    companion object {
        private val log = KotlinLogging.logger { }
    }

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

    private val tempDir = createTempDir("wavebeans-facilitator")
    private val terminate = CountDownLatch(1)
    private val startupClasses = startUpClasses()
    private val jobStates = ConcurrentHashMap<JobKey, JobState>()
    private var startedFrom: List<StackTraceElement>? = null
    private var communicator: io.grpc.Server? = null

    fun start(): Facilitator {
        if (startedFrom != null)
            throw IllegalStateException("Facilitator $this is already started from: " +
                    startedFrom!!.joinToString("\n") { "\tat $it" })
        startedFrom = Thread.currentThread().stackTrace.toList()

        ExecutionConfig.podCallResultBuilder(podCallResultBuilder)
        ExecutionConfig.mediumBuilder(mediumBuilder)
        ExecutionConfig.executionThreadPool(executionThreadPool)

        communicatorPort?.let {
            communicator = ServerBuilder.forPort(it)
                    .addService(TableGrpcService.instance(TableRegistry.default))
                    .addService(FacilitatorGrpcService.instance(this))
                    .addService(MetricGrpcService.instance())
                    .build()
                    .start()
            log.info { "Communicator on port $it started." }
        }

        return this
    }

    fun startupClasses(): List<ClassDesc> = startupClasses

    fun registerCode(jobKey: JobKey, jarFileStream: InputStream) {
        val codeFile = createTempFile("code-$jobKey", "jar", tempDir)
        FileOutputStream(codeFile).use {
            it.write(jarFileStream.readBytes())
        }
        jobStates(jobKey).classLoader += codeFile.toURI().toURL()
    }

    fun plantBush(request: io.wavebeans.communicator.PlantBushRequest) {
        val jobKey = request.jobKey.toJobKey()
        gardener.plantBush(
                jobKey,
                request.jobContent.bushKey.toBushKey(),
                jsonCompact(SerializersModule { tableQuery(); beanParams() }).decodeFromString(
                        ListSerializer(PodRef.serializer()),
                        request.jobContent.podsAsJson
                ),
                request.sampleRate
        )
        jobStates(jobKey).futures.addAll(gardener.getAllFutures(jobKey))
    }

    fun registerBushEndpoints(registerBushEndpointsRequest: io.wavebeans.communicator.RegisterBushEndpointsRequest) {
        val remoteBushes = jobStates(registerBushEndpointsRequest.jobKey.toJobKey()).remoteBushes
        registerBushEndpointsRequest.bushEndpointsList.forEach { bushEndpoint ->
            val bushKey = bushEndpoint.bushKey.toBushKey()
            val remoteBush = RemoteBush(bushKey, bushEndpoint.location)
            podDiscovery.registerBush(bushKey, remoteBush)
            remoteBushes.add(remoteBush)
            bushEndpoint.podsList.forEach {
                podDiscovery.registerPod(bushKey, PodKey(it.id, it.partition))
            }
        }
    }

    fun startJob(jobKey: JobKey) {
        gardener.start(jobKey)
    }

    fun jobs(): List<JobKey> {
        return gardener.jobs()
    }

    fun describeJob(jobKey: JobKey): List<io.wavebeans.communicator.JobContent> {
        fun stringify(l: List<PodRef>): String =
                jsonCompact(SerializersModule { tableQuery(); beanParams() })
                        .encodeToString(ListSerializer(PodRef.serializer()), l)
        return gardener.job(jobKey).map {
            io.wavebeans.communicator.JobContent.newBuilder()
                    .setBushKey(it.bushKey.toString())
                    .setPodsAsJson(stringify(it.podRefs))
                    .build()
        }
    }

    fun status(jobKey: JobKey): List<JobStatusResponse.JobStatus> {
        return gardener.getAllFutures(jobKey).map { future ->
            JobStatusResponse.JobStatus.newBuilder()
                    .setJobKey(jobKey.toString())
                    .apply {
                        when {
                            future.isDone -> {
                                try {
                                    val result = future.get(5000, TimeUnit.MILLISECONDS)
                                    status = if (result.exception == null) DONE else FAILED
                                    result.exception?.let {
                                        hasException = true
                                        exception = it.toExceptionObj()
                                    }
                                } catch (e: ExecutionException) {
                                    status = FAILED
                                    hasException = true
                                    exception = (e.cause ?: e).toExceptionObj()
                                }
                            }
                            future.isCancelled -> status = CANCELLED
                            else -> status = IN_PROGRESS
                        }
                    }.build()
        }
    }

    fun call(bushKey: BushKey, podKey: PodKey, request: String): InputStream {
        val bush = podDiscovery.bush(bushKey) ?: throw IllegalStateException("Bush $bushKey not found")
        if (bush !is LocalBush) throw IllegalStateException("Bush $bushKey is ${bush::class} but ${LocalBush::class} expected")
        val podCallResult = bush.call(podKey, request).get(callTimeoutMillis, TimeUnit.MILLISECONDS)
        return podCallResult.stream()
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
        if (
                communicator
                        ?.shutdown()
                        ?.awaitTermination(onServerShutdownTimeoutMillis, TimeUnit.MILLISECONDS) == false
        ) {
            communicator?.shutdownNow()
        }
    }

    private fun jobStates(jobKey: JobKey): JobState {
        return jobStates.computeIfAbsent(jobKey) { JobState.create(jobKey) }
    }
}


