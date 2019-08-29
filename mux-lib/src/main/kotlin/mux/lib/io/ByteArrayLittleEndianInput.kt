package mux.lib.io

import mux.lib.*
import mux.lib.stream.FiniteSampleStream
import mux.lib.stream.SampleStream
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

fun Iterable<Int>.sampleStream(sampleRate: Float, bitDepth: BitDepth = BitDepth.BIT_8): SampleStream {
    return FiniteSampleStream(
            ByteArrayLittleEndianInput(sampleRate, bitDepth, this.map {
                when (bitDepth) {
                    BitDepth.BIT_8 -> it.toByte()
                    BitDepth.BIT_16 -> TODO()
                    BitDepth.BIT_24 -> TODO()
                    BitDepth.BIT_32 -> TODO()
                    BitDepth.BIT_64 -> TODO()
                }
            }.toList().toByteArray())
    )
}

class ByteArrayLittleEndianEncoder(val sampleRate: Float, val bitDepth: BitDepth) {

    fun sequence(sampleRate: Float, buffer: ByteArray): Sequence<Sample> {
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

class ByteArrayLittleEndianInput(val sampleRate: Float, val bitDepth: BitDepth, val buffer: ByteArray) : FiniteInput {

    override fun length(timeUnit: TimeUnit): Long = samplesCountToLength(samplesCount().toLong(), sampleRate, timeUnit)

    override fun samplesCount(): Int = buffer.size / bitDepth.bytesPerSample

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): FiniteInput {
        val s = max(timeToSampleIndexFloor(start, timeUnit, sampleRate), 0).toInt()
        val e = end?.let { min(timeToSampleIndexCeil(end, timeUnit, sampleRate).toInt(), samplesCount()) }
                ?: samplesCount()
        return ByteArrayLittleEndianInput(
                sampleRate,
                bitDepth,
                // TODO this should be done without copying of underlying buffer
                buffer = buffer.copyOfRange(
                        s * bitDepth.bytesPerSample,
                        e * bitDepth.bytesPerSample
                )
        )
    }

    override fun asSequence(sampleRate: Float): Sequence<Sample> =
            ByteArrayLittleEndianEncoder(this.sampleRate, this.bitDepth).sequence(sampleRate, this.buffer)

}
