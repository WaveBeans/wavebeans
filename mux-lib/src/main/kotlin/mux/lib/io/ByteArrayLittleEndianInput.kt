package mux.lib.io

import mux.lib.*
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

class ByteArrayLittleEndianAudioInput(val sampleRate: Float, val bitDepth: BitDepth, val buffer: ByteArray) : AudioInput {

    override fun length(timeUnit: TimeUnit): Long = samplesCountToLength(sampleCount().toLong(), sampleRate, timeUnit)

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): AudioInput {
        val s = max(timeToSampleIndexFloor(start, timeUnit, sampleRate), 0).toInt()
        val e = end?.let { min(timeToSampleIndexCeil(end, timeUnit, sampleRate).toInt(), sampleCount()) }
                ?: sampleCount()
        return ByteArrayLittleEndianAudioInput(
                sampleRate,
                bitDepth,
                buffer.copyOfRange(
                        s * bitDepth.bytesPerSample,
                        e * bitDepth.bytesPerSample
                )
        )
    }

    override fun info(namespace: String?): Map<String, String> {
        val prefix = namespace?.let { "[$it] " } ?: ""
        return mapOf(
                "${prefix}Bit depth" to "${bitDepth.bits} bit",
                "${prefix}Size" to "${buffer.size} bytes"
        )
    }

    override fun sampleCount(): Int = buffer.size / bitDepth.bytesPerSample

    override fun asSequence(sampleRate: Float): Sequence<Sample> {
        if (sampleRate != this.sampleRate) throw UnsupportedOperationException("Can't resample from ${this.sampleRate} to $sampleRate")
        return buffer.iterator().asSequence()
                .windowed(bitDepth.bytesPerSample, bitDepth.bytesPerSample) { bytes ->
                    when (bitDepth) {
                        BitDepth.BIT_8 -> sampleOf(bytes[0])
                        BitDepth.BIT_16 -> sampleOf(
                                bytes.foldIndexed(0) { index, acc, v ->
                                    acc or (v.toInt() and 0xFF shl index * 8)
                                }.toShort()
                        )
                        BitDepth.BIT_24, BitDepth.BIT_32 -> sampleOf(
                                bytes.foldIndexed(0) { index, acc, v ->
                                    acc or (v.toInt() and 0xFF shl index * 8)
                                },
                                as24bit = bitDepth == BitDepth.BIT_24
                        )
                        BitDepth.BIT_64 -> sampleOf(
                                bytes.foldIndexed(0L) { index, acc, v ->
                                    acc or (v.toLong() and 0xFF shl index * 8)
                                }
                        )
                    }
                }
    }
}

