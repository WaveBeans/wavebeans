package io.wavebeans.lib.io

import io.wavebeans.lib.*
import io.wavebeans.metrics.clazzTag
import io.wavebeans.metrics.samplesProcessedOnInputMetric
import kotlinx.serialization.Serializable
import kotlin.math.cos
import kotlin.reflect.jvm.jvmName

fun Number.sine(
        /** Amplitude of the resulted sine, by default 1.0 */
        amplitude: Double = 1.0,
        /** Time offset in seconds of the resulted sine, by default 0.0*/
        timeOffset: Double = 0.0
): BeanStream<Sample> {
    return SineGeneratedInput(SineGeneratedInputParams(this.toDouble(), amplitude, timeOffset))
}

@Serializable
data class SineGeneratedInputParams(
        /** Frequency of the sinusoid. */
        val frequency: Double,
        /** Amplitude of the sinusoid. 0.0 < a <= 1.0 */
        val amplitude: Double,
        /** Time offset in seconds. */
        val timeOffset: Double = 0.0,
        /** Length of the sinusoid, when you read after that moment stream will just return zeros, in seconds */
        val time: Double? = null
) : BeanParams

class SineGeneratedInput constructor(
        override val parameters: SineGeneratedInputParams
) : AbstractInputBeanStream<Sample>(), SourceBean<Sample>, SinglePartitionBean {

    private val samplesProcessed = samplesProcessedOnInputMetric.withTags(clazzTag to SineGeneratedInput::class.jvmName)

    override fun inputSequence(sampleRate: Float): Sequence<Sample> {
        return object : Iterator<Sample> {

            private var x = parameters.timeOffset
            private val delta = 1.0 / sampleRate // sinusoid automatically resamples to output sample rate

            override fun hasNext(): Boolean = true

            override fun next(): Sample {
                return if (parameters.time != null && x >= parameters.time + parameters.timeOffset) {
                    ZeroSample
                } else {
                    val r = sampleOf(sineOf(x))
                    x += delta
                    r
                }.also { samplesProcessed.increment() }
            }
        }.asSequence()
    }

    private fun sineOf(x: Double): Double =
            parameters.amplitude * cos(x * 2 * Math.PI * parameters.frequency)

    override val desiredSampleRate: Float? = null

}