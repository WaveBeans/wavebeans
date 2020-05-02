package io.wavebeans.execution.distributed

import kotlinx.serialization.KSerializer
import kotlinx.serialization.protobuf.ProtoBuf
import mu.KotlinLogging
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.Inflater

var serializationCompression = true
var serializationCompressionLevel = Deflater.BEST_COMPRESSION
var serializationLogTracing = false

private val log = KotlinLogging.logger {}

internal fun <T: Any> T.asByteArray(serializer: KSerializer<T>): ByteArray =
        try {
            val buf = ProtoBuf(encodeDefaults = false).dump(serializer, this)
            val result = if (serializationCompression) {
                val baos = ByteArrayOutputStream()
                try {
                    val dfl = Deflater()
                    dfl.setLevel(serializationCompressionLevel)
                    dfl.setInput(buf)
                    dfl.finish()
                    val tmp = ByteArray(4096)
                    while (!dfl.finished()) {
                        val size = dfl.deflate(tmp)
                        baos.write(tmp, 0, size)
                    }
                } finally {
                    baos.close()
                }
                baos.toByteArray()
            } else {
                buf
            }
            log.trace {
                "[serializer=$serializer,serializationCompression=$serializationCompression," +
                        "serializationCompressionLevel=$serializationCompressionLevel] ${this::class}" +
                        " serialized (${result.size}bytes)" +
                        if (serializationLogTracing)
                            ", as hex:\n" + result.asSequence()
                                    .windowed(40, 40, true)
                                    .joinToString("\n") {
                                        it.joinToString(" ") {
                                            (it.toInt().and(0xFF)).toString(16).padStart(2, '0')
                                        }
                                    }
                        else ""
            }
            result

        } catch (e: Throwable) {
            if (e is OutOfMemoryError) throw e // most likely no resources to handle. Just fail
            throw IllegalStateException("Can't serialize with `$serializer` object: $this", e)
        }

internal fun <T> ByteArray.asObj(serializer: KSerializer<T>): T =
        try {
            val buf = if (serializationCompression) {
                val baos = ByteArrayOutputStream()
                val inflater = Inflater()
                inflater.setInput(this)
                val tmp = ByteArray(4096)
                while (!inflater.finished()) {
                    val size = inflater.inflate(tmp)
                    baos.write(tmp, 0, size)
                }
                baos.toByteArray()
            } else {
                this
            }
            ProtoBuf(encodeDefaults = false).load(serializer, buf)
        } catch (e: Throwable) {
            if (e is OutOfMemoryError) throw e // most likely no resources to handle. Just fail
            throw IllegalStateException("Can't deserialize with `$serializer` the buffer " +
                    "as hex (${this.size}bytes):\n${
                    this.asSequence()
                            .windowed(40, 40, true)
                            .joinToString("\n") {
                                it.joinToString(" ") {
                                    (it.toInt().and(0xFF)).toString(16).padStart(2, '0')
                                }
                            }
                    }", e)
        }