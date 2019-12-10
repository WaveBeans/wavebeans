package io.wavebeans.lib.stream

import io.wavebeans.lib.*
import kotlinx.serialization.Serializable

operator fun BeanStream<Sample>.times(multiplier: Double): BeanStream<Sample> = this.changeAmplitude(multiplier)
operator fun BeanStream<Sample>.div(divisor: Double): BeanStream<Sample> = this.changeAmplitude(1.0 / divisor)

fun BeanStream<Sample>.changeAmplitude(multiplier: Double): BeanStream<Sample> =
        ChangeAmplitudeSampleStream(this, ChangeAmplitudeSampleStreamParams(multiplier))

@Serializable
data class ChangeAmplitudeSampleStreamParams(
        val multiplier: Double
) : BeanParams()

class ChangeAmplitudeSampleStream(
        val source: BeanStream<Sample>,
        val params: ChangeAmplitudeSampleStreamParams
) : BeanStream<Sample>, SingleBean<Sample> {

    override val parameters: BeanParams = params

    override val input: Bean<Sample> = source

    override fun asSequence(sampleRate: Float): Sequence<Sample> {
        return source.asSequence(sampleRate).map { it * params.multiplier }
    }
}