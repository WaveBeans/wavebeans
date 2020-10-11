package io.wavebeans.lib.io

import io.wavebeans.lib.*
import io.wavebeans.metrics.clazzTag
import io.wavebeans.metrics.samplesProcessedOnInputMetric
import java.util.concurrent.TimeUnit
import kotlin.reflect.jvm.jvmName

class ByteArrayLittleEndianDecoder(
        val sampleRate: Float,
        val bitDepth: BitDepth
) {

    fun sequence(sampleRate: Float, buffer: ByteArray): Sequence<Sample> {
        if (sampleRate != this.sampleRate) throw UnsupportedOperationException("Can't resample from ${this.sampleRate} to $sampleRate")

        var bufferPos = 0

        return object : Iterator<Sample> {
            override fun hasNext(): Boolean = bufferPos + bitDepth.bytesPerSample <= buffer.size

            override fun next(): Sample {
                return when (bitDepth) {
                    BitDepth.BIT_8 -> sampleOf(buffer[bufferPos].asUnsignedByte().toByte())
                    BitDepth.BIT_16 -> sampleOf(
                            ((buffer[bufferPos].toInt() and 0xFF) or
                                    (buffer[bufferPos + 1].toInt() and 0xFF shl 8)
                                    ).toShort()
                    )
                    BitDepth.BIT_24 -> sampleOf(
                            ((buffer[bufferPos].toInt() and 0xFF) or
                                    (buffer[bufferPos + 1].toInt() and 0xFF shl 8) or
                                    (buffer[bufferPos + 2].toInt() and 0xFF shl 16)
                                    ).let {
                                        if (it and 0x800000 != 0) -(it.inv() and 0x7FFFFF)
                                        else it and 0x7FFFFF
                                    },
                            as24bit = true
                    )
                    BitDepth.BIT_32 -> sampleOf(
                            ((buffer[bufferPos].toInt() and 0xFF) or
                                    (buffer[bufferPos + 1].toInt() and 0xFF shl 8) or
                                    (buffer[bufferPos + 2].toInt() and 0xFF shl 16) or
                                    (buffer[bufferPos + 3].toInt() and 0xFF shl 24)
                                    )
                    )
                    BitDepth.BIT_64 -> sampleOf(
                            ((buffer[bufferPos].toLong() and 0xFF) or
                                    (buffer[bufferPos + 1].toLong() and 0xFF shl 8) or
                                    (buffer[bufferPos + 2].toLong() and 0xFF shl 16) or
                                    (buffer[bufferPos + 3].toLong() and 0xFF shl 24) or
                                    (buffer[bufferPos + 4].toLong() and 0xFF shl 32) or
                                    (buffer[bufferPos + 5].toLong() and 0xFF shl 40) or
                                    (buffer[bufferPos + 6].toLong() and 0xFF shl 48) or
                                    (buffer[bufferPos + 7].toLong() and 0xFF shl 46)
                                    )
                    )
                }.also { bufferPos += bitDepth.bytesPerSample }
            }

        }.asSequence()
    }
}

data class ByteArrayLittleEndianInputParams(val sampleRate: Float, val bitDepth: BitDepth, val buffer: ByteArray) : BeanParams()

class ByteArrayLittleEndianInput(val params: ByteArrayLittleEndianInputParams) : FiniteInput<Sample>, SinglePartitionBean {

    private val samplesProcessed = samplesProcessedOnInputMetric.withTags(clazzTag to ByteArrayLittleEndianInput::class.jvmName)

    override val parameters: BeanParams = params

    override fun length(timeUnit: TimeUnit): Long = samplesCountToLength(samplesCount().toLong(), params.sampleRate, timeUnit)

    override fun samplesCount(): Int = params.buffer.size / params.bitDepth.bytesPerSample

    override fun asSequence(sampleRate: Float): Sequence<Sample> =
            ByteArrayLittleEndianDecoder(params.sampleRate, params.bitDepth)
                    .sequence(sampleRate, params.buffer)
                    .map { samplesProcessed.increment(); it }

}
