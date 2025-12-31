package io.wavebeans.lib.io

import io.wavebeans.lib.BeanParams
import io.wavebeans.lib.BitDepth
import io.wavebeans.lib.Sample
import io.wavebeans.lib.SinglePartitionBean
import io.wavebeans.lib.TimeUnit
import io.wavebeans.lib.samplesCountToLength
import io.wavebeans.lib.stream.FiniteStream
import kotlinx.serialization.Serializable

data class ByteArrayLittleEndianInputParams(
        val sampleRate: Float,
        val bitDepth: BitDepth,
        val buffer: ByteArray
) : BeanParams

class ByteArrayLittleEndianInput(
        val params: ByteArrayLittleEndianInputParams
) : AbstractInputBeanStream<Sample>(), FiniteStream<Sample>, SinglePartitionBean {

//    private val samplesProcessed = samplesProcessedOnInputMetric.withTags(clazzTag to ByteArrayLittleEndianInput::class.jvmName)

    override val parameters: BeanParams = params

    override val desiredSampleRate: Float? = params.sampleRate

    override fun length(timeUnit: TimeUnit): Long = samplesCountToLength(samplesCount().toLong(), params.sampleRate, timeUnit)

    override fun samplesCount(): Long = (params.buffer.size / params.bitDepth.bytesPerSample).toLong()

    override fun inputSequence(sampleRate: Float): Sequence<Sample> {
        require(sampleRate == params.sampleRate) { "The stream should be resampled from ${params.sampleRate}Hz to ${sampleRate}Hz" }
        return ByteArrayLittleEndianDecoder(params.bitDepth)
                .sequence(params.buffer)
//                .map { samplesProcessed.increment(); it }
    }

}
