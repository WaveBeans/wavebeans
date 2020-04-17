package io.wavebeans.execution.distributed

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
import io.ktor.routing.delete
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.serialization.json
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.getOrFail
import io.wavebeans.execution.*
import io.wavebeans.execution.config.ExecutionConfig
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
import java.net.InetAddress
import java.net.URL
import java.net.URLClassLoader
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Future
import java.util.jar.JarFile
import kotlin.random.Random
import kotlin.random.nextInt

fun main() {
    val advertisingHostAddress = "10.0.0.42"
    val listeningPortRange = 40000..40000
    val startingUpAttemptsCount = 10
    val threadsNum = 4

    CrewGardener(
            advertisingHostAddress,
            listeningPortRange,
            startingUpAttemptsCount,
            threadsNum
    ).start(waitAndClose = true)
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
        private val gardener: Gardener = Gardener()
) : Closeable {

    init {
        ExecutionConfig.executionThreadPool(MultiThreadedExecutionThreadPool(threadsNumber))
    }

    companion object {
        private val log = KotlinLogging.logger { }
    }

    val tempDir = createTempDir("wavebeans-crew-gardener")

    private val terminate = CountDownLatch(1)

    private lateinit var server: ApplicationEngine

    private val jobClassLoaders = ConcurrentHashMap<JobKey, CrewGardenerClassLoader>()
    private val jobFutures = ConcurrentHashMap<JobKey, MutableList<Future<ExecutionResult>>>()

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

        var listeningPort = Random.nextInt(listeningPortRange)
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
        if (attempt == 0) {
            log.error { "Can't start the Crew Gardener within $startingUpAttemptsCount attempts. Exiting" }
            return
        }

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
        gardener.cancelAll()
        terminate.countDown()
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
            server.stop(5000, 5000)
    }

    fun cancelJob(jobKey: JobKey) {
        gardener.cancel(jobKey)
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
}

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
        post("/bush") {
            val plantBushRequest = call.receive(PlantBushRequest::class)
            log.trace { "/bush\nRequest body: $plantBushRequest" }
            crewGardener.plantBush(plantBushRequest)
            call.respond("OK")
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
        delete("/job") {
            val jobKey = call.request.queryParameters.getOrFail<String>("jobKey").toJobKey()
            crewGardener.cancelJob(jobKey)
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
