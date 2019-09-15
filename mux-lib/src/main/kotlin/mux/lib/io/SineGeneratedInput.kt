package mux.lib.io

import mux.lib.Sample
import mux.lib.ZeroSample
import mux.lib.sampleOf
import mux.lib.stream.SampleStreamException
import java.util.concurrent.TimeUnit
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min


class SineGeneratedInput private constructor(
        /** Frequency of the sinusoid. */
        val frequency: Double,
        /** Amplitude of the sinusoid. 0.0 < a <= 1.0 */
        val amplitude: Double,
        /** time offset. */
        val timeOffset: Double = 0.0,
        /** Length of the sinusoid, when you read after that moment stream will just return zeros. */
        val time: Double? = null
) : StreamInput {

    constructor(frequency: Double, amplitude: Double, timeOffset: Double = 0.0) : this(frequency, amplitude, timeOffset, null)

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): StreamInput {
        if (end != null && end <= start) throw SampleStreamException("End=[$end] should be greater than start=[$start]")
        val s = max(timeUnit.toNanos(start) / 1_000_000_000.0, 0.0)
        val e = end?.let { min(timeUnit.toNanos(end) / 1_000_000_000.0, time ?: Double.MAX_VALUE) }
        return if (e != null)
            SineGeneratedInput(frequency, amplitude, s + timeOffset, time = e - s)
        else
            SineGeneratedInput(frequency, amplitude, s + timeOffset)
    }

    override fun asSequence(sampleRate: Float): Sequence<Sample> {
        return object : Iterator<Sample> {

            private var x = timeOffset
            private val delta = 1.0 / sampleRate // sinusoid automatically resamples to output sample rate

            override fun hasNext(): Boolean = true

            override fun next(): Sample {
                if (time != null && x >= time + timeOffset) return ZeroSample
                val r = sampleOf(sineOf(x))
                x += delta
                return r
            }
        }.asSequence()
    }

    private fun sineOf(x: Double): Double =
            amplitude * cos(x * 2 * Math.PI * frequency)

}