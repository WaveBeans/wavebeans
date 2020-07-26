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
import io.wavebeans.metrics.*
import mu.KotlinLogging
import java.io.BufferedOutputStream
import java.io.InputStream
import java.util.*
import java.util.concurrent.LinkedTransferQueue
import java.util.concurrent.TimeUnit

enum class AudioStreamOutputFormat(val id: String, val contentType: ContentType) {
    WAV("wav", ContentType("audio", "wav"))
    ;

    companion object {
        fun byId(id: String): AudioStreamOutputFormat? = values().firstOrNull { it.id.toLowerCase() == id.toLowerCase() }
    }
}


fun Application.audioService(tableRegistry: TableRegistry) {
    val audioService = AudioService(tableRegistry)

    routing {
        get("/audio/{tableName}/stream/{format}") {
            val tableName = call.parameters.required("tableName") { it }
            val format = call.parameters.required("format") { AudioStreamOutputFormat.byId(it) }
            val bitDepth = call.request.queryParameters.optional("bitDepth") {
                BitDepth.safelyOf(it.toInt()) ?: throw BadRequestException("Bit depth $it is not recognized")
            } ?: BitDepth.BIT_16
            val limit = call.request.queryParameters.optional("limit") {
                TimeMeasure.parseOrNull(it) ?: throw BadRequestException("Limit $it can't be parsed")
            }
            val offset = call.request.queryParameters.optional("offset") {
                TimeMeasure.parseOrNull(it) ?: throw BadRequestException("Offset $it can't be parsed")
            }

            if (!audioService.tableRegistry.exists(tableName)) throw NotFoundException("$tableName is not found")

            call.respondOutputStream(format.contentType) {
                BufferedOutputStream(this).use { buffer ->
                    audioService.stream<Any>(format, tableName, bitDepth, limit, offset).use {
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

class AudioService(internal val tableRegistry: TableRegistry) {
    companion object {
        private val log = KotlinLogging.logger { }

    }

    fun <T : Any> stream(
            format: AudioStreamOutputFormat,
            tableName: String,
            bitDepth: BitDepth,
            limit: TimeMeasure?,
            offset: TimeMeasure?
    ): InputStream {
        val metricTags = arrayOf(
                tableTag to tableName,
                bitDepthTag to bitDepth.bits.toString(),
                formatTag to format.contentType.toString(),
                limitTag to (limit?.toString() ?: "n/a"),
                offsetTag to (offset?.toString() ?: "n/a")
        )
        audioStreamRequestMetric.withTags(*metricTags).increment()
        val _startTime = System.currentTimeMillis()
        val table = tableRegistry.byName<T>(tableName)
        return when (format) {
            AudioStreamOutputFormat.WAV -> streamAsWav(table, bitDepth, limit, offset)
        }.also {
            audioStreamRequestTimeMetric.withTags(*metricTags).time(System.currentTimeMillis() - _startTime)
        }
    }

    private fun <T : Any> streamAsWav(
            table: TimeseriesTableDriver<T>,
            bitDepth: BitDepth,
            limit: TimeMeasure?,
            offset: TimeMeasure?
    ): InputStream {
        val audioStreamBytesSentMetricObj = audioStreamBytesSentMetric.withTags(
                tableTag to table.tableName,
                bitDepthTag to bitDepth.bits.toString(),
                formatTag to "wav",
                limitTag to (limit?.toString() ?: "n/a"),
                offsetTag to (offset?.toString() ?: "n/a")
        )
        val nextBytes: Queue<Byte> = LinkedTransferQueue<Byte>()
        val writerDelegate = object : WriterDelegate() {
            override fun write(b: Int) {
                nextBytes.add(b.toByte())
            }
        }

        val sampleRate = table.sampleRate
        val tableType = table.tableType
        val writer: Writer =
                @Suppress("UNCHECKED_CAST")
                when (tableType) {
                    Sample::class -> WavWriter(
                            (table as TimeseriesTableDriver<Sample>).stream(offset ?: 0.s)
                                    .let { if (limit != null) it.trim(limit.ns(), TimeUnit.NANOSECONDS) else it },
                            bitDepth,
                            sampleRate,
                            1,
                            writerDelegate
                    )
                    SampleArray::class -> WavWriterFromSampleArray(
                            (table as TimeseriesTableDriver<SampleArray>).stream(offset ?: 0.s)
                                    .let { if (limit != null) it.trim(limit.ns(), TimeUnit.NANOSECONDS) else it },
                            bitDepth,
                            sampleRate,
                            1,
                            writerDelegate
                    )
                    else -> throw UnsupportedOperationException("Table type $tableType is not supported for audio streaming")
                }

        val header = WavHeader(bitDepth, sampleRate, 1, Int.MAX_VALUE).header()
        var headerIdx = 0

        return object : InputStream() {
            override fun read(): Int {
                return try {
                    if (headerIdx < header.size) {
                        header[headerIdx++].toInt() and 0xFF
                    } else {
                        if (nextBytes.isEmpty()) writer.write()

                        if (nextBytes.isNotEmpty()) (nextBytes.poll().toInt() and 0xFF)
                        else -1
                    }
                } catch (e: ClassCastException) {
                    log.error(e) { "Table $table has wrong type" }
                    -1
                }.also { if (it != -1) audioStreamBytesSentMetricObj.increment() }
            }
        }
    }
}