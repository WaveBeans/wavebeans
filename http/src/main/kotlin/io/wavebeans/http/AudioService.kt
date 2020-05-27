package io.wavebeans.http

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.features.BadRequestException
import io.ktor.features.NotFoundException
import io.ktor.http.ContentType
import io.ktor.response.respondOutputStream
import io.ktor.routing.get
import io.ktor.routing.routing
import io.wavebeans.lib.*
import io.wavebeans.lib.io.*
import io.wavebeans.lib.stream.trim
import io.wavebeans.lib.table.TableRegistry
import io.wavebeans.lib.table.TimeseriesTableDriver
import mu.KotlinLogging
import java.io.BufferedOutputStream
import java.io.InputStream
import java.io.Serializable
import java.util.*
import java.util.concurrent.LinkedTransferQueue
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

enum class AudioStreamOutputFormat(val id: String, val contentType: ContentType) {
    WAV("wav", ContentType("audio", "wav"))
    ;

    companion object {
        fun byId(id: String): AudioStreamOutputFormat? = values().firstOrNull { it.id.toLowerCase() == id.toLowerCase() }
    }
}


fun Application.audioService() {
    val audioService = AudioService()

    routing {
        get("/audio/{tableName}/stream/{format}") {
            val tableName = call.parameters.required("tableName") { it }
            val format = call.parameters.required("format") { AudioStreamOutputFormat.byId(it) }
            val sampleRate = call.request.queryParameters.optional("sampleRate") { it.toFloat() } ?: 44100.0f
            val bitDepth = call.request.queryParameters.optional("bitDepth") {
                BitDepth.safelyOf(it.toInt()) ?: throw BadRequestException("Bit depth $it is not recognized")
            } ?: BitDepth.BIT_16
            val clazz = when (val sourceType = call.request.queryParameters.optional("sourceType") { it }) {
                null -> Sample::class
                "sample" -> Sample::class
                "sampleArray" -> SampleArray::class
                else -> throw BadRequestException("$sourceType is not supported")
            }
            val limit = call.request.queryParameters.optional("limit") {
                TimeMeasure.parseOrNull(it) ?: throw BadRequestException("Limit $it can't be parsed")
            }

            if (!audioService.tableRegistry.exists(tableName)) throw NotFoundException("$tableName is not found")

            call.respondOutputStream(format.contentType) {
                BufferedOutputStream(this).use { buffer ->
                    audioService.stream(format, tableName, sampleRate, bitDepth, clazz, limit).use {
                        while (true) {
                            val b = it.read()
                            if (b < 0) break;
                            buffer.write(b)
                        }
                    }
                }
            }
        }
    }
}

class AudioService(
        internal val tableRegistry: TableRegistry = TableRegistry.instance()
) {
    companion object {
        private val log = KotlinLogging.logger { }
    }

    fun <T : Any> stream(
            format: AudioStreamOutputFormat,
            tableName: String,
            sampleRate: Float,
            bitDepth: BitDepth,
            elementClazz: KClass<T>,
            limit: TimeMeasure?
    ): InputStream {
        val table = tableRegistry.byName<T>(tableName)
        return when (format) {
            AudioStreamOutputFormat.WAV -> streamAsWav(table, sampleRate, bitDepth, elementClazz, limit)
        }
    }

    private fun <T : Any> streamAsWav(
            table: TimeseriesTableDriver<T>,
            sampleRate: Float,
            bitDepth: BitDepth,
            elementClazz: KClass<T>,
            limit: TimeMeasure?
    ): InputStream {

        val nextBytes: Queue<Byte> = LinkedTransferQueue<Byte>()
        val writerDelegate = object : WriterDelegate() {
            override fun write(b: Int) {
                nextBytes.add(b.toByte())
            }
        }

        val writer: Writer =
                @Suppress("UNCHECKED_CAST")
                when (elementClazz) {
                    Sample::class -> WavWriter(
                            (table as TimeseriesTableDriver<Sample>).stream(0.s)
                                    .let { if (limit != null) it.trim(limit.asNanoseconds(), TimeUnit.NANOSECONDS) else it },
                            bitDepth,
                            sampleRate,
                            1,
                            writerDelegate
                    )
                    SampleArray::class -> WavWriterFromSampleArray(
                            (table as TimeseriesTableDriver<SampleArray>).stream(0.s)
                                    .let { if (limit != null) it.trim(limit.asNanoseconds(), TimeUnit.NANOSECONDS) else it },
                            bitDepth,
                            sampleRate,
                            1,
                            writerDelegate
                    )
                    else -> throw UnsupportedOperationException("$elementClazz is not supported for audio streaming")
                }

        val header = WavHeader(bitDepth, sampleRate, 1, Int.MAX_VALUE).header()
        var headerIdx = 0

        return object : InputStream() {
            override fun read(): Int {
                try {
                    if (headerIdx < header.size) {
                        return header[headerIdx++].toInt() and 0xFF
                    }
                    if (nextBytes.isEmpty()) writer.write()
                    return if (nextBytes.isNotEmpty()) (nextBytes.poll().toInt() and 0xFF) else -1
                } catch (e: ClassCastException) {
                    log.error(e) { "Table $table has wrong type" }
                    return -1
                }
            }
        }
    }
}