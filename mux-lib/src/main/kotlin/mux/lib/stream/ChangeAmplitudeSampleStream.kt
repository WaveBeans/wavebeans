package mux.lib.stream

import kotlinx.serialization.Serializable
import mux.lib.Bean
import mux.lib.BeanParams
import mux.lib.SingleBean
import mux.lib.Sample
import java.util.concurrent.TimeUnit

fun SampleStream.changeAmplitude(multiplier: Double): SampleStream =
        ChangeAmplitudeSampleStream(this, ChangeAmplitudeSampleStreamParams(multiplier))

@Serializable
data class ChangeAmplitudeSampleStreamParams(
        val multiplier: Double
) : BeanParams()

class ChangeAmplitudeSampleStream(
        val source: SampleStream,
        val params: ChangeAmplitudeSampleStreamParams
) : SampleStream, SingleBean<Sample, SampleStream> {

    override val parameters: BeanParams = params

    override val input: Bean<Sample, SampleStream> = source

    override fun asSequence(sampleRate: Float): Sequence<Sample> {
        return source.asSequence(sampleRate).map { it * params.multiplier }
    }

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): SampleStream {
        return ChangeAmplitudeSampleStream(source.rangeProjection(start, end, timeUnit), params)
    }
}