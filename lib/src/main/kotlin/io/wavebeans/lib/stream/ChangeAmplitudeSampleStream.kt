package io.wavebeans.lib.stream

import io.wavebeans.lib.*
import kotlinx.serialization.Serializable
import java.util.concurrent.TimeUnit

operator fun SampleStream.times(multiplier: Double): SampleStream = this.changeAmplitude(multiplier)
operator fun SampleStream.div(divisor: Double): SampleStream = this.changeAmplitude(1.0 / divisor)

fun SampleStream.changeAmplitude(multiplier: Double): SampleStream =
        ChangeAmplitudeSampleStream(this, ChangeAmplitudeSampleStreamParams(multiplier))

@Serializable
data class ChangeAmplitudeSampleStreamParams(
        val multiplier: Double
) : BeanParams()

class ChangeAmplitudeSampleStream(
        val source: SampleStream,
        val params: ChangeAmplitudeSampleStreamParams
) : SampleStream, SingleBean<SampleArray, SampleStream> {

    override val parameters: BeanParams = params

    override val input: Bean<SampleArray, SampleStream> = source

    override fun asSequence(sampleRate: Float): Sequence<SampleArray> {
        return source.asSequence(sampleRate)
                .map { arr -> createSampleArray(arr.size) { arr[it] * params.multiplier } }
    }

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): SampleStream {
        return ChangeAmplitudeSampleStream(source.rangeProjection(start, end, timeUnit), params)
    }
}