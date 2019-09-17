package mux.lib.io

import mux.lib.MuxParams
import mux.lib.Sample
import mux.lib.ZeroSample
import mux.lib.sampleOf
import mux.lib.stream.SampleStream
import mux.lib.stream.SampleStreamException
import mux.lib.stream.sampleStream
import java.util.concurrent.TimeUnit
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min

fun Number.sine(
        amplitude: Double = 1.0,
        timeOffset: Double = 0.0
): SampleStream {
    return SineGeneratedInput(SineGeneratedInputParams(this.toDouble(), amplitude, timeOffset)).sampleStream()
}

data class SineGeneratedInputParams(
        /** Frequency of the sinusoid. */
        val frequency: Double,
        /** Amplitude of the sinusoid. 0.0 < a <= 1.0 */
        val amplitude: Double,
        /** time offset. */
        val timeOffset: Double = 0.0,
        /** Length of the sinusoid, when you read after that moment stream will just return zeros. */
        val time: Double? = null
) : MuxParams

class SineGeneratedInput constructor(
        val params: SineGeneratedInputParams
) : StreamInput {
    override val parameters: MuxParams = params

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): StreamInput {
        if (end != null && end <= start) throw SampleStreamException("End=[$end] should be greater than start=[$start]")
        val s = max(timeUnit.toNanos(start) / 1_000_000_000.0, 0.0)
        val e = end?.let { min(timeUnit.toNanos(end) / 1_000_000_000.0, params.time ?: Double.MAX_VALUE) }
        return if (e != null)
            SineGeneratedInput(params.copy(timeOffset = s + params.timeOffset, time = e - s))
        else
            SineGeneratedInput(params.copy(timeOffset = s + params.timeOffset))
    }

    override fun asSequence(sampleRate: Float): Sequence<Sample> {
        return object : Iterator<Sample> {

            private var x = params.timeOffset
            private val delta = 1.0 / sampleRate // sinusoid automatically resamples to output sample rate

            override fun hasNext(): Boolean = true

            override fun next(): Sample {
                if (params.time != null && x >= params.time + params.timeOffset) return ZeroSample
                val r = sampleOf(sineOf(x))
                x += delta
                return r
            }
        }.asSequence()
    }

    private fun sineOf(x: Double): Double =
            params.amplitude * cos(x * 2 * Math.PI * params.frequency)

}