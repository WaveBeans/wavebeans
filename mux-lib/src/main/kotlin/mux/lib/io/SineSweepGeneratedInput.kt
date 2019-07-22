package mux.lib.io

import mux.lib.stream.Sample
import mux.lib.stream.sampleOf
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.*
import kotlin.math.abs
import kotlin.math.cos


class SineSweepGeneratedInput(
        /** Sample rate in Hz for generated sinusoid. */
        val sampleRate: Float,
        /** Start frequency of the sinusoid sweep. */
        val startFrequency: Double,
        /** End frequency of the sinusoid sweep. */
        val endFrequency: Double,
        /** Amplitude of the sinusoid. 0.0 < a <= 1.0 */
        val amplitude: Double,
        /** Number of seconds to generate. */
        val time: Double,
        /** time offset. */
        val timeOffset: Double = 0.0,
        /** Frequency will be changed by this value evenly. Make sure sample rate allowes this. It shouldn't be less than (1 / sample rate) */
        val sweepDelta: Double = 0.1

) : AudioInput {
    override fun length(timeUnit: TimeUnit): Long = timeUnit.convert((time * 1_000_000_000.0).toLong(), NANOSECONDS)

    private val samplesCount = (time * sampleRate).toInt()

    override fun info(namespace: String?): Map<String, String> {
        val prefix = namespace?.let { "[$it] " } ?: ""
        return mapOf(
                "${prefix}Sinusoid amplitude" to "$amplitude",
                "${prefix}Sinusoid length" to "${time}sec",
                "${prefix}Sinusoid offset" to "${timeOffset}sec",
                "${prefix}Sinusoid start frequency" to "${startFrequency}Hz",
                "${prefix}Sinusoid end frequency" to "${endFrequency}Hz"
        )
    }

    override fun sampleCount(): Int {
        return samplesCount
    }

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): AudioInput {
        TODO()
//        if (end != null && end <= start) throw SampleStreamException("End=[$end] should be greater than start=[$start]")
//        val s = max(timeUnit.toNanos(start) / 1_000_000_000.0, 0.0)
//        val e = end?.let { min(timeUnit.toNanos(end) / 1_000_000_000.0, time) } ?: time
//        val newLength = e - s
//        return SineSweepGeneratedInput(sampleRate, frequency, amplitude, newLength, s + timeOffset)
    }

    override fun asSequence(sampleRate: Float): Sequence<Sample> {
        return object : Iterator<Sample> {

            private var x = timeOffset
            private var frequency = startFrequency
            private var transitionCounter = 0.0
            private val step = 1.0 / sampleRate // sinusoid automatically resamples to output sample rate
            private val frequencyDelta = time / (endFrequency - startFrequency) / sweepDelta

            override fun hasNext(): Boolean = x < time + timeOffset

            override fun next(): Sample {
                if (!hasNext()) NoSuchElementException("")
                val r = sampleOf(sineOf(x, frequency))
                x += step
                if (abs(transitionCounter - frequencyDelta) < step) {
                    frequency += sweepDelta
                    transitionCounter = 0.0
                } else {
                    transitionCounter += step
                }

                return r
            }
        }.asSequence()
    }

    private fun sineOf(x: Double, frequency: Double): Double =
            amplitude * cos(x * 2 * Math.PI * frequency)

}