package mux.lib.io

import mux.lib.*
import mux.lib.stream.SampleStreamException
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.cos

// TODO it should infinite stream

fun ClosedRange<Int>.sineSweep(amplitude: Double, time: Double, timeOffset: Double = 0.0, sweepDelta: Double = 0.1): StreamInput =
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
) : BeanParams()

class SineSweepGeneratedInput(
        val params: SineSweepGeneratedInputParams
) : StreamInput {

    override val parameters: BeanParams = params

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): StreamInput {
        TODO()
//        if (end != null && end <= start) throw SampleStreamException("End=[$end] should be greater than start=[$start]")
//        val s = max(timeUnit.toNanos(start) / 1_000_000_000.0, 0.0)
//        val e = end?.let { min(timeUnit.toNanos(end) / 1_000_000_000.0, time) } ?: time
//        val newLength = e - s
//        return SineSweepGeneratedInput(sampleRate, frequency, amplitude, newLength, s + timeOffset)
    }

    override fun asSequence(sampleRate: Float): Sequence<SampleArray> {
        return object : Iterator<SampleArray> {

            private var x = params.timeOffset
            private var frequency = params.startFrequency
            private var transitionCounter = 0.0
            private val step = 1.0 / sampleRate // sinusoid automatically resamples to output sample rate
            private val frequencyDelta = params.time / (params.endFrequency - params.startFrequency) / params.sweepDelta

            override fun hasNext(): Boolean = true

            override fun next(): SampleArray {
                return createSampleArray {
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

                    r
                }
            }
        }.asSequence()
    }

    private fun sineOf(x: Double, frequency: Double): Double =
            params.amplitude * cos(x * 2 * Math.PI * frequency)

}