package io.wavebeans.execution.distributed

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.ConfigSpec
import com.uchuhimo.konf.source.Source
import com.uchuhimo.konf.source.hocon
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.request.receive
import io.ktor.request.receiveMultipart
import io.ktor.response.respond
import io.ktor.response.respondOutputStream
import io.ktor.routing.*
import io.ktor.serialization.json
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.getOrFail
import io.wavebeans.execution.*
import io.wavebeans.execution.config.ExecutionConfig
import io.wavebeans.execution.medium.MediumBuilder
import io.wavebeans.execution.medium.PodCallResultBuilder
import io.wavebeans.execution.pod.PodKey
import io.wavebeans.lib.WaveBeansClassLoader
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.modules.plus
import kotlinx.serialization.modules.serializersModuleOf
import mu.KotlinLogging
import java.io.Closeable
import java.io.File
import java.io.OutputStream
import java.lang.Exception
import java.net.InetAddress
import java.net.URL
import java.net.URLClassLoader
import java.rmi.Remote
import java.util.*
import java.util.concurrent.*
import java.util.jar.JarFile
import kotlin.random.Random
import kotlin.random.nextInt

object CrewGardenderConfig : ConfigSpec() {
    val advertisingHostAddress by optional("127.0.0.1", name = "advertisingHostAddress")
    val listeningPortRange by required<IntRange>(name = "listeningPortRange")
    val startingUpAttemptsCount by optional(10, name = "startingUpAttemptsCount")
    val threadsNumber by required<Int>(name = "threadsNumber")
    val callTimeoutMillis by optional(5000L, name = "callTimeoutMillis")
    val onServerShutdownGracePeriodMillis by optional(5000L, name = "onServerShutdownGracePeriodMillis")
    val onServerShutdownTimeoutMillis by optional(5000L, name = "onServerShutdownTimeoutMillis")
}


fun main(args: Array<String>) {
    if (args.isEmpty()) {
        throw IllegalArgumentException("Specify configuration file as a parameter.")
    }
    val log = KotlinLogging.logger { }
    val configFilePath = args[0]

    val config = Config { addSpec(CrewGardenderConfig) }.withSource(Source.from.hocon.file(configFilePath))

    log.info {
        "Staring Crew Gardener with following config:n\"" +
                config.toMap().entries.joinToString("\n") { "${it.key} = ${it.value}" }
    }


    val crewGardener = CrewGardener(
            advertisingHostAddress = config[CrewGardenderConfig.advertisingHostAddress],
            listeningPortRange = config[CrewGardenderConfig.listeningPortRange],
            startingUpAttemptsCount = config[CrewGardenderConfig.startingUpAttemptsCount],
            threadsNumber = config[CrewGardenderConfig.threadsNumber],
            callTimeoutMillis = config[CrewGardenderConfig.callTimeoutMillis],
            onServerShutdownGracePeriodMillis = config[CrewGardenderConfig.onServerShutdownGracePeriodMillis],
            onServerShutdownTimeoutMillis = config[CrewGardenderConfig.onServerShutdownTimeoutMillis]
    )

    Runtime.getRuntime().addShutdownHook(Thread {
        log.info { "Got interruption signal. Terminating Crew Gardener" }
        crewGardener.terminate()
    })

    crewGardener.start(waitAndClose = true)

}

class CrewGardenerClassLoader(parent: ClassLoader) : URLClassLoader(emptyArray(), parent) {
    operator fun plusAssign(url: URL) {
        addURL(url)
    }
}

class CrewGardener(
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
    }

    val tempDir = createTempDir("wavebeans-crew-gardener")

    private val terminate = CountDownLatch(1)

    private lateinit var server: ApplicationEngine
    private val jobClassLoaders = ConcurrentHashMap<JobKey, CrewGardenerClassLoader>()
    private val jobFutures = ConcurrentHashMap<JobKey, MutableList<Future<ExecutionResult>>>()
    private val remoteBushes = ConcurrentHashMap<JobKey, MutableList<RemoteBush>>()
    private var listeningPort: Int = 0

    fun listeningPort(): Int =
            if (listeningPort > 0) listeningPort
            else throw IllegalStateException("Crew Gardener is not listening yet. Please start first")

    fun start(waitAndClose: Boolean) {
        log.info {
            "Advertising on host:\n" +
                    InetAddress.getAllByName(advertisingHostAddress).joinToString("\n") {
                        """
                        canonicalHostName = ${it.canonicalHostName}
                        hostAddress = ${it.hostAddress}
                        hostName = ${it.hostName}
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
                            "$listeningPortRange. Left $attempt attempts before quit."
                }
                listeningPort = Random.nextInt(listeningPortRange)
            }
        }
        if (attempt == 0) throw IllegalStateException("Can't start the Crew Gardener within $startingUpAttemptsCount attempts.")


        log.info { "Crew Gardener API started on port $listeningPort" }
        if (waitAndClose) {
            terminate.await()
            log.info { "Crew Gardener terminating" }
            close()
            log.info { "Crew Gardener stopped" }
        }
    }

    private fun startApi(serverPort: Int) {
        if (applicationEngine == null) {
            val env = applicationEngineEnvironment {
                module {
                    crewGardenerApi(this@CrewGardener)
                }
                connector {
                    host = "0.0.0.0"
                    port = serverPort
                }
            }
            server = embeddedServer(Netty, env).start()
        } else {
            applicationEngine.application.crewGardenerApi(this)
        }
    }

    fun terminate() {
        log.info { "Terminating" }
        gardener.stopAll()
        terminate.countDown()
        remoteBushes.values.flatten().forEach {
            it.close()
            podDiscovery.unregisterBush(it.bushKey)
        }
    }

    fun plantBush(request: PlantBushRequest) {
        gardener.plantBush(request.jobKey, request.jobContent.bushKey, request.jobContent.pods, request.sampleRate)
        jobFutures.putIfAbsent(request.jobKey, CopyOnWriteArrayList<Future<ExecutionResult>>())
        jobFutures.getValue(request.jobKey).addAll(gardener.getAllFutures(request.jobKey))
    }

    fun job(jobKey: JobKey): List<JobContent> =
            gardener.job(jobKey).map { JobContent(it.bushKey, it.podRefs) }

    override fun close() {
        if (applicationEngine == null)
            server.stop(onServerShutdownGracePeriodMillis, onServerShutdownTimeoutMillis)
    }

    fun startJob(jobKey: JobKey) {
        gardener.start(jobKey)
    }

    fun stopJob(jobKey: JobKey) {
        gardener.stop(jobKey)
        remoteBushes[jobKey]?.forEach {
            it.close()
            podDiscovery.unregisterBush(it.bushKey)
        }
        remoteBushes.remove(jobKey)
    }

    fun job(): List<JobKey> {
        return gardener.jobs()
    }

    fun registerCode(jobKey: JobKey, codeFile: File) {
        log.info {
            "[JobKey $jobKey] Registering code from jar $codeFile. Code file content:\n" +
                    JarFile(codeFile).entries().asSequence().joinToString("\n") { "${it.name} size=${it.size}bytes crc=${it.crc}" }
        }
        jobClassLoaders.putIfAbsent(jobKey, CrewGardenerClassLoader(this::class.java.classLoader))
        val cl = jobClassLoaders.getValue(jobKey)
        cl += codeFile.toURI().toURL()
        WaveBeansClassLoader.addClassLoader(cl)
    }

    fun registerBushEndpoints(registerBushEndpointsRequest: RegisterBushEndpointsRequest) {
        remoteBushes.putIfAbsent(registerBushEndpointsRequest.jobKey, CopyOnWriteArrayList())
        val remoteBushes = remoteBushes.getValue(registerBushEndpointsRequest.jobKey)
        registerBushEndpointsRequest.bushEndpoints.forEach { bushEndpoint ->
            val bushKey = bushEndpoint.bushKey
            val remoteBush = RemoteBush(bushKey, bushEndpoint.location)
            podDiscovery.registerBush(bushKey, remoteBush)
            remoteBushes.add(remoteBush)
            bushEndpoint.pods.forEach { podDiscovery.registerPod(bushKey, it) }
        }
    }

    fun call(bushKey: BushKey, podKey: PodKey, request: String): (OutputStream) -> Unit {
        val bush = podDiscovery.bush(bushKey) ?: throw IllegalStateException("Bush $bushKey not found")
        if (bush !is LocalBush) throw IllegalStateException("Bush $bushKey is ${bush::class} but ${LocalBush::class} expected")
        val podCallResult = bush.call(podKey, request).get(callTimeoutMillis, TimeUnit.MILLISECONDS)
        return podCallResult::writeTo
    }

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
}

enum class FutureStatus {
    IN_PROGRESS,
    DONE,
    CANCELLED,
    FAILED
}

@Serializable
data class JobStatus(
        @Serializable(with = UUIDSerializer::class)
        val jobKey: JobKey,
        val status: FutureStatus,
        val exception: ExceptionObj?
)

@Serializable
data class JobContent(
        @Serializable(with = UUIDSerializer::class)
        val bushKey: BushKey,
        val pods: List<PodRef>
)

@Serializable
data class PlantBushRequest(
        @Serializable(with = UUIDSerializer::class)
        val jobKey: JobKey,
        val jobContent: JobContent,
        val sampleRate: Float
)

@Serializable
data class RegisterBushEndpointsRequest(
        @Serializable(with = UUIDSerializer::class)
        val jobKey: JobKey,
        val bushEndpoints: List<BushEndpoint>
)

@Serializable
data class BushEndpoint(
        @Serializable(with = UUIDSerializer::class)
        val bushKey: BushKey,
        val location: String,
        val pods: List<PodKey>
)

fun Application.crewGardenerApi(crewGardener: CrewGardener) {

    val log = KotlinLogging.logger { }
    val json = Json(configuration = JsonConfiguration.Stable)

    install(ContentNegotiation) {
        json(
                json = JsonConfiguration.Stable,
                module = serializersModuleOf(UUID::class, UUIDSerializer) + // though that doesn't work :\
                        TopologySerializer.paramsModule
        )
    }
    install(CallLogging)

    routing {
        get("/status") {
            call.respond("OK")
        }
        post("/bush") {
            val plantBushRequest = call.receive(PlantBushRequest::class)
            log.debug { "/bush\nRequest body: $plantBushRequest" }
            crewGardener.plantBush(plantBushRequest)
            call.respond("OK")
        }
        post("/bush/endpoints") {
            val registerBushEndpointsRequest = call.receive(RegisterBushEndpointsRequest::class)
            log.debug { "/bush/endpoints\nRequest body: $registerBushEndpointsRequest" }
            crewGardener.registerBushEndpoints(registerBushEndpointsRequest)
            call.respond("OK")
        }
        get("/bush/call") {
            val bushKey = call.request.queryParameters.getOrFail<String>("bushKey").toBushKey()
            val podKey = PodKey(
                    call.request.queryParameters.getOrFail<Long>("podId"),
                    call.request.queryParameters.getOrFail<Int>("podPartition")
            )
            val request = call.request.queryParameters.getOrFail<String>("request")
            try {
                val write = crewGardener.call(bushKey, podKey, request)
                call.respondOutputStream { write(this) }
            } catch (e: Exception) {
                log.error(e) { "Error during call to bushKey=$bushKey, podKey=$podKey, request=$request" }
                call.respond(HttpStatusCode.InternalServerError, e.message ?: "Internal error")
            }
        }

        get("/job") {
            val jobKey = call.request.queryParameters.getOrFail<String>("jobKey").toJobKey()
            val jobContents = crewGardener.job(jobKey)
            if (jobContents.isNotEmpty())
                call.respond(jobContents)
            else
                call.respond(HttpStatusCode.NotFound, "Not found")
        }
        get("/jobs") {
            call.respond(json.stringify(ListSerializer(UUIDSerializer), crewGardener.job()))
        }
        get("/job/status") {
            val jobKey = call.request.queryParameters.getOrFail<String>("jobKey").toJobKey()
            call.respond(json.stringify(ListSerializer(JobStatus.serializer()), crewGardener.status(jobKey)))
        }
        delete("/job") {
            val jobKey = call.request.queryParameters.getOrFail<String>("jobKey").toJobKey()
            crewGardener.stopJob(jobKey)
            call.respond("OK")
        }
        put("/job") {
            val jobKey = call.request.queryParameters.getOrFail<String>("jobKey").toJobKey()
            crewGardener.startJob(jobKey)
            call.respond("OK")
        }

        get("/terminate") {
            crewGardener.terminate()
            call.respond("Terminating")
        }

        post("/code/upload") {
            val multipart = call.receiveMultipart()
            lateinit var jobKey: JobKey
            multipart.forEachPart { part ->
                when {
                    part is PartData.FormItem && part.name == "jobKey" -> jobKey = part.value.toJobKey()
                    part is PartData.FileItem -> {
                        val codeFile = File(crewGardener.tempDir, "code-$jobKey-${System.currentTimeMillis()}.jar")
                        part.streamProvider().use { upload ->
                            codeFile.outputStream().buffered().use {
                                upload.copyTo(it)
                            }
                        }
                        crewGardener.registerCode(jobKey, codeFile)
                    }
                }
                part.dispose()
            }
            call.respond("OK")
        }
    }
}
