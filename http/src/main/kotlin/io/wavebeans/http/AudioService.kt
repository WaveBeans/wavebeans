package io.wavebeans.http

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.features.BadRequestException
import io.ktor.http.ContentType
import io.ktor.response.respondOutputStream
import io.ktor.routing.get
import io.ktor.routing.routing
import io.wavebeans.lib.*
import io.wavebeans.lib.io.BufferedWriter
import io.wavebeans.lib.io.WavWriter
import io.wavebeans.lib.table.TableRegistry
import java.io.BufferedOutputStream

fun Application.audioService() {
    val audioService = AudioService()

    routing {
        get("/audio/{tableName}/stream") {
            val tableName: String = call.parameters.required("tableName") { it }

            call.respondOutputStream(
                    contentType = ContentType("audio", "wav")
            ) {
                val buffer = BufferedOutputStream(this)
                audioService.streamAsWav(tableName)
                        .forEach { buffer.write(it.toInt()) }
            }
        }
    }
}

class AudioService(
        private val tableRegistry: TableRegistry = TableRegistry.instance()
) {
    fun streamAsWav(tableName: String): Sequence<Byte> {
        val table = try {
            tableRegistry.byName<Sample>(tableName)
        } catch (e: ClassCastException) {
            throw BadRequestException(e.message ?: "unknown cast")
        }

        val sampleRate = 44100.0f
        val bitDepth = BitDepth.BIT_8
        val numberOfChannels = 1

        var sample: Int? = null
        val bufferedWriter = object : BufferedWriter() {
            override fun write(b: Int) {
                sample = b
            }
        }
        val writer = object : WavWriter(
                table.stream(0.s),
                bitDepth,
                sampleRate,
                numberOfChannels,
                bufferedWriter
        ) {

            override var dataSize: Int = Int.MAX_VALUE

            override fun incDataSize(value: Int) {
                // doing nothing
            }
        }

        val header = bufferedWriter.headerFn() ?: ByteArray(0)
        var headerIdx = 0

        return object : Iterator<Byte> {
            override fun hasNext(): Boolean {
                if (headerIdx < header.size) return true
                return if (sample != null) true else writer.write()
            }

            override fun next(): Byte {
                if (headerIdx < header.size) {
                    return header[headerIdx++]
                }
                if (sample == null) writer.write()
                val s = sample ?: throw NoSuchElementException("No more elements")
                sample = null
                return s.toByte()
            }

        }.asSequence()
    }
}