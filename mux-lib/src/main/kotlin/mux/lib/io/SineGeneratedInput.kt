package mux.lib.io

import mux.lib.stream.Sample
import mux.lib.stream.SampleStreamException
import mux.lib.stream.sampleOf
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.*
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min


class SineGeneratedInput(
        /** Sample rate in Hz for generated sinusoid. */
        val sampleRate: Float,
        /** Frequency of the sinusoid. */
        val frequency: Double,
        /** Amplitude of the sinusoid. 0.0 < a <= 1.0 */
        val amplitude: Double,
        /** Number of seconds to generate. */
        val time: Double,
        /** time offset. */
        val timeOffset: Double = 0.0
) : AudioInput {
    override fun length(timeUnit: TimeUnit): Long = timeUnit.convert((time * 1000.0).toLong(), MILLISECONDS)

    private val samplesCount = (time * sampleRate).toInt()

    override fun info(namespace: String?): Map<String, String> {
        val prefix = namespace?.let { "[$it] " } ?: ""
        return mapOf(
                "${prefix}Sinusoid amplitude" to "$amplitude",
                "${prefix}Sinusoid length" to "${time}sec",
                "${prefix}Sinusoid offset" to "${timeOffset}sec",
                "${prefix}Sinusoid frequency" to "${frequency}Hz"
        )
    }

    override fun size(): Int {
        return samplesCount
    }

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): AudioInput {
        if (end != null && end <= start) throw SampleStreamException("End=[$end] should be greater than start=[$start]")
        val s = max(timeUnit.toNanos(start) / 1_000_000_000.0, 0.0)
        val e = end?.let { min(timeUnit.toNanos(end) / 1_000_000_000.0, time) } ?: time
        val newLength = e - s
        return SineGeneratedInput(sampleRate, frequency, amplitude, newLength, s + timeOffset)
    }

    override fun asSequence(sampleRate: Float): Sequence<Sample> {
        return object : Iterator<Sample> {

            private var x = timeOffset
            private val delta = 1.0 / sampleRate // sinusoid automatically resamples to output sample rate

            override fun hasNext(): Boolean = x < time + timeOffset

            override fun next(): Sample {
                if (!hasNext()) NoSuchElementException("")
                val r = sampleOf(sineOf(x))
                x += delta
                return r
            }
        }.asSequence()
    }

    private fun sineOf(x: Double): Double =
            amplitude * cos(x * 2 * Math.PI * frequency)

}