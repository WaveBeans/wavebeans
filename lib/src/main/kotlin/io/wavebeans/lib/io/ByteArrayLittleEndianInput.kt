package io.wavebeans.lib.io

import io.wavebeans.lib.*
import io.wavebeans.metrics.clazzTag
import io.wavebeans.metrics.samplesProcessedOnInputMetric
import kotlinx.serialization.Serializable
import java.util.concurrent.TimeUnit
import kotlin.reflect.jvm.jvmName

@Serializable
data class ByteArrayLittleEndianInputParams(
        val sampleRate: Float,
        val bitDepth: BitDepth,
        val buffer: ByteArray
) : BeanParams()

class ByteArrayLittleEndianInput(
        val params: ByteArrayLittleEndianInputParams
) : AbstractInputBeanStream<Sample>(), FiniteInput<Sample>, SinglePartitionBean {

    private val samplesProcessed = samplesProcessedOnInputMetric.withTags(clazzTag to ByteArrayLittleEndianInput::class.jvmName)

    override val parameters: BeanParams = params

    override val desiredSampleRate: Float? = params.sampleRate

    override fun length(timeUnit: TimeUnit): Long = samplesCountToLength(samplesCount().toLong(), params.sampleRate, timeUnit)

    override fun samplesCount(): Int = params.buffer.size / params.bitDepth.bytesPerSample

    override fun inputSequence(sampleRate: Float): Sequence<Sample> {
        require(sampleRate == params.sampleRate) { "The stream should be resampled from ${params.sampleRate}Hz to ${sampleRate}Hz" }
        return ByteArrayLittleEndianDecoder(params.bitDepth)
                .sequence(params.buffer)
                .map { samplesProcessed.increment(); it }
    }

}
