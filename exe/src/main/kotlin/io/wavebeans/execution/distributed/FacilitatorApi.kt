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
import io.ktor.response.respondOutputStream
import io.ktor.routing.*
import io.ktor.serialization.json
import io.ktor.server.engine.BaseApplicationResponse
import io.ktor.util.getOrFail
import io.wavebeans.execution.*
import io.wavebeans.execution.pod.PodKey
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.modules.plus
import kotlinx.serialization.modules.serializersModuleOf
import mu.KotlinLogging
import java.io.File
import java.io.PrintStream
import java.util.*

fun Application.facilitatorApi(facilitator: Facilitator) {

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
            facilitator.plantBush(plantBushRequest)
            call.respond("OK")
        }
        post("/bush/endpoints") {
            val registerBushEndpointsRequest = call.receive(RegisterBushEndpointsRequest::class)
            log.debug { "/bush/endpoints\nRequest body: $registerBushEndpointsRequest" }
            facilitator.registerBushEndpoints(registerBushEndpointsRequest)
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
                val write = facilitator.call(bushKey, podKey, request)
                call.respondOutputStream {
                    try {
                        write(this)
                    } catch (e: Exception) {
                        val printStream = PrintStream(this)
                        printStream.println("-----ERROR----")
                        e.printStackTrace(printStream)
                    }
                }
            } catch (e: Exception) {
                log.error(e) { "Error during call to bushKey=$bushKey, podKey=$podKey, request=$request" }
                call.respond(HttpStatusCode.InternalServerError, e.message ?: "Internal error")
            }
        }
        get("/job") {
            val jobKey = call.request.queryParameters.getOrFail<String>("jobKey").toJobKey()
            val jobContents = facilitator.job(jobKey)
            if (jobContents.isNotEmpty())
                call.respond(jobContents)
            else
                call.respond(HttpStatusCode.NotFound, "Not found")
        }
        get("/jobs") {
            call.respond(json.stringify(ListSerializer(UUIDSerializer), facilitator.job()))
        }
        get("/job/status") {
            val jobKey = call.request.queryParameters.getOrFail<String>("jobKey").toJobKey()
            call.respond(json.stringify(ListSerializer(JobStatus.serializer()), facilitator.status(jobKey)))
        }
        delete("/job") {
            val jobKey = call.request.queryParameters.getOrFail<String>("jobKey").toJobKey()
            facilitator.stopJob(jobKey)
            call.respond("OK")
        }
        put("/job") {
            val jobKey = call.request.queryParameters.getOrFail<String>("jobKey").toJobKey()
            facilitator.startJob(jobKey)
            call.respond("OK")
        }
        get("/terminate") {
            facilitator.terminate()
            call.respond("Terminating")
        }
        post("/code/upload") {
            val multipart = call.receiveMultipart()
            lateinit var jobKey: JobKey
            multipart.forEachPart { part ->
                when {
                    part is PartData.FormItem && part.name == "jobKey" -> jobKey = part.value.toJobKey()
                    part is PartData.FileItem -> {
                        val codeFile = File(facilitator.tempDir, "code-$jobKey-${System.currentTimeMillis()}.jar")
                        part.streamProvider().use { upload ->
                            codeFile.outputStream().buffered().use {
                                upload.copyTo(it)
                            }
                        }
                        facilitator.registerCode(jobKey, codeFile)
                    }
                }
                part.dispose()
            }
            call.respond("OK")
        }
        get("/code/classes") {
            call.respond(facilitator.startupClasses())
        }
    }
}
