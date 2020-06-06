package io.wavebeans.http

import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.BadRequestException
import io.ktor.features.DataConversion
import io.ktor.features.MissingRequestParameterException
import io.ktor.features.ParameterConversionException
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.response.respond
import io.ktor.response.respondOutputStream
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.util.*
import io.ktor.util.getOrFail
import io.wavebeans.lib.TimeMeasure
import io.wavebeans.lib.s
import io.wavebeans.lib.table.TableRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.OutputStream
import java.lang.reflect.Type
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.cast
import kotlin.reflect.jvm.jvmName
import kotlin.reflect.typeOf

fun Application.tableService(tableRegistry: TableRegistry) {
    val tableService = TableService(tableRegistry)

    routing {
        get("/table/{tableName}/last") {
            val tableName: String = call.parameters.required("tableName") { it }
            val interval: TimeMeasure = call.request.queryParameters.required("interval") { TimeMeasure.parseOrNull(it) }

            if (tableService.exists(tableName)) {
                val stream = tableService.last(tableName, interval)
                call.respondOutputStream {
                    streamOutput(stream)
                }
            } else {
                call.respond(HttpStatusCode.NotFound, "$tableName is not found")
            }
        }
        get("/table/{tableName}/timeRange") {
            val tableName: String = call.parameters.required("tableName") { it }
            val from: TimeMeasure = call.request.queryParameters.required("from") { TimeMeasure.parseOrNull(it) }
            val to: TimeMeasure = call.request.queryParameters.required("to") { TimeMeasure.parseOrNull(it) }

            if (tableService.exists(tableName)) {
                val stream = tableService.timeRange(tableName, from, to)
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

class TableService(private val tableRegistry: TableRegistry) {

    fun exists(tableName: String): Boolean = tableRegistry.exists(tableName)

    fun last(tableName: String, interval: TimeMeasure): InputStream =
            if (tableRegistry.exists(tableName)) {
                val table = tableRegistry.byName<Any>(tableName)
                JsonBeanStreamReader(
                        stream = table.last(interval),
                        sampleRate = table.sampleRate,
                        offset = table.lastMarker() ?: 0.s
                )
            } else {
                ByteArrayInputStream(ByteArray(0))
            }


    fun timeRange(tableName: String, from: TimeMeasure, to: TimeMeasure): InputStream =
            if (tableRegistry.exists(tableName)) {
                val table = tableRegistry.byName<Any>(tableName)
                JsonBeanStreamReader(
                        stream = table.timeRange(from, to),
                        sampleRate = table.sampleRate,
                        offset = from
                )
            } else {
                ByteArrayInputStream(ByteArray(0))
            }
}

fun <T : Any> Parameters.required(name: String, converter: (String) -> T?): T =
        optional(name, converter) ?: throw BadRequestException("$name can't be converted")

fun <T : Any> Parameters.optional(name: String, converter: (String) -> T?): T? = this[name]?.let { converter(it) }