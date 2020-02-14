package io.wavebeans.http

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.response.respondOutputStream
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.util.KtorExperimentalAPI
import io.ktor.util.getOrFail
import io.wavebeans.lib.TimeMeasure
import io.wavebeans.lib.s
import io.wavebeans.lib.table.TableRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit

@KtorExperimentalAPI
fun Application.tableService() {
    val tableService = TableService()
    routing {
        get("/table/{tableName}/last/{interval}/{sampleRate?}") {
            val tableName = call.parameters.getOrFail("tableName")
            val interval = call.parameters.getOrFail("interval").let { TimeMeasure.parse(it) }
            val sampleRate = call.parameters["sampleRate"]?.toFloat() ?: 44100.0f
            if (tableService.exists(tableName)) {
                val stream = tableService.last(tableName, interval, sampleRate)
                call.respondOutputStream {
                    streamOutput(stream)
                }
            } else {
                call.respond(HttpStatusCode.NotFound, "$tableName is not found")
            }
        }
        get("/table/{tableName}/timeRange/{from}/{to}/{sampleRate?}") {
            val tableName = call.parameters.getOrFail("tableName")
            val from = call.parameters.getOrFail("from").let { TimeMeasure.parse(it) }
            val to = call.parameters.getOrFail("to").let { TimeMeasure.parse(it) }
            val sampleRate = call.parameters["sampleRate"]?.toFloat() ?: 44100.0f
            if (tableService.exists(tableName)) {
                val stream = tableService.timeRange(tableName, from, to, sampleRate)
                call.respondOutputStream {
                    streamOutput(stream)
                }
            } else {
                call.respond(HttpStatusCode.NotFound, "$tableName is not found")
            }
        }
    }
}

private suspend fun OutputStream.streamOutput(stream: InputStream) {
    withContext(Dispatchers.IO) {
        val buf = ByteArray(1024)
        var flushCounter = 0L
        do {
            val r = stream.read(buf)
            if (r > 0) {
                write(buf, 0, r)
            }
            if (flushCounter++ % 32L == 0L) {
                flush()
            }
        } while (r > 0)
    }
}

class TableService(
        private val tableRegistry: TableRegistry = TableRegistry.instance()
) {

    fun exists(tableName: String): Boolean = tableRegistry.exists(tableName)

    fun last(tableName: String, interval: TimeMeasure, sampleRate: Float): InputStream =
            if (tableRegistry.exists(tableName)) {
                JsonBeanStreamReader(
                        stream = tableRegistry.byName<Any>(tableName).last(interval),
                        sampleRate = sampleRate,
                        offset = tableRegistry.byName<Any>(tableName).lastMarker() ?: 0.s
                )
            } else {
                ByteArrayInputStream(ByteArray(0))
            }


    fun timeRange(tableName: String, from: TimeMeasure, to: TimeMeasure, sampleRate: Float): InputStream =
            if (tableRegistry.exists(tableName)) {
                JsonBeanStreamReader(
                        stream = tableRegistry.byName<Any>(tableName).timeRange(from, to),
                        sampleRate = sampleRate,
                        offset = from
                )
            } else {
                ByteArrayInputStream(ByteArray(0))
            }
}