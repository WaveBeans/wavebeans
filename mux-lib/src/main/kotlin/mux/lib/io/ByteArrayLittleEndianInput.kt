package mux.lib.io

import mux.lib.*
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

class ByteArrayLittleEndianDecoder(val sampleRate: Float, val bitDepth: BitDepth) {

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

data class ByteArrayLittleEndianInputParams(val sampleRate: Float, val bitDepth: BitDepth, val buffer: ByteArray) : MuxParams

class ByteArrayLittleEndianInput(val params: ByteArrayLittleEndianInputParams) : FiniteInput {

    override val parameters: MuxParams = params

    override fun length(timeUnit: TimeUnit): Long = samplesCountToLength(samplesCount().toLong(), params.sampleRate, timeUnit)

    override fun samplesCount(): Int = params.buffer.size / params.bitDepth.bytesPerSample

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): FiniteInput {
        val s = max(timeToSampleIndexFloor(start, timeUnit, params.sampleRate), 0).toInt()
        val e = end?.let { min(timeToSampleIndexCeil(end, timeUnit, params.sampleRate).toInt(), samplesCount()) }
                ?: samplesCount()
        return ByteArrayLittleEndianInput(
                params.copy(
                        // TODO this should be done without copying of underlying buffer
                        buffer = params.buffer.copyOfRange(
                                s * params.bitDepth.bytesPerSample,
                                e * params.bitDepth.bytesPerSample
                        )
                )
        )
    }

    override fun asSequence(sampleRate: Float): Sequence<Sample> =
            ByteArrayLittleEndianDecoder(params.sampleRate, params.bitDepth).sequence(sampleRate, params.buffer)

}
