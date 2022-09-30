package io.wavebeans.lib.io

import io.wavebeans.lib.*
import io.wavebeans.metrics.clazzTag
import io.wavebeans.metrics.samplesProcessedOnInputMetric
import kotlin.math.abs
import kotlin.math.cos
import kotlin.reflect.jvm.jvmName

/**
 * Creates [SineSweepGeneratedInputParams].
 *
 * @param amplitude is [SineSweepGeneratedInputParams.amplitude]
 * @param time is [SineSweepGeneratedInputParams.time]
 * @param timeOffset is [SineSweepGeneratedInputParams.timeOffset]
 * @param sweepDelta is [SineSweepGeneratedInputParams.sweepDelta]
 */
fun ClosedRange<Int>.sineSweep(amplitude: Double, time: Double, timeOffset: Double = 0.0, sweepDelta: Double = 0.1): BeanStream<Sample> =
        SineSweepGeneratedInput(SineSweepGeneratedInputParams(
                this.start.toDouble(),
                this.endInclusive.toDouble(),
                amplitude,
                time,
                timeOffset,
                sweepDelta
        ))

data class SineSweepGeneratedInputParams(
        /** Start frequency of the sinusoid sweep. */
        val startFrequency: Double,
        /** End frequency of the sinusoid sweep. */
        val endFrequency: Double,
        /** Amplitude of the sinusoid. 0.0 < a <= 1.0 */
        val amplitude: Double,
        /** Number of seconds to get the end frequency. */
        val time: Double,
        /** time offset. */
        val timeOffset: Double = 0.0,
        /** Frequency will be changed by this value evenly. Make sure sample rate allows this. It shouldn't be less than (1 / sample rate) */
        val sweepDelta: Double = 0.1
) : BeanParams

/**
 * Generates the sine that changes the frequency from start to end within specified perion of time
 */
class SineSweepGeneratedInput(
        val params: SineSweepGeneratedInputParams
) : BeanStream<Sample>, SourceBean<Sample>, SinglePartitionBean {

    private val samplesProcessed = samplesProcessedOnInputMetric.withTags(clazzTag to SineSweepGeneratedInput::class.jvmName)

    override val parameters: BeanParams = params

    override fun asSequence(sampleRate: Float): Sequence<Sample> {
        return object : Iterator<Sample> {

            private var x = params.timeOffset
            private var frequency = params.startFrequency
            private var transitionCounter = 0.0
            private val step = 1.0 / sampleRate // sinusoid automatically resamples to output sample rate
            private val frequencyDelta = params.time / (params.endFrequency - params.startFrequency) / params.sweepDelta

            override fun hasNext(): Boolean = true

            override fun next(): Sample {
                val r = sampleOf(sineOf(x, frequency))
                x += step
                if (x < params.time + params.timeOffset) {
                    if (abs(transitionCounter - frequencyDelta) < step) {
                        frequency += params.sweepDelta
                        transitionCounter = 0.0
                    } else {
                        transitionCounter += step
                    }
                }
                samplesProcessed.increment()
                return r
            }
        }.asSequence()
    }

    private fun sineOf(x: Double, frequency: Double): Double =
            params.amplitude * cos(x * 2 * Math.PI * frequency)

    override val desiredSampleRate: Float? = null

}