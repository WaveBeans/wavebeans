package mux.lib.io

import mux.lib.stream.Sample
import mux.lib.stream.sampleOf
import kotlin.math.cos


class SineGeneratedInput(
        /** Sample rate in Hz for generated sinusoid. */
        val sampleRate: Float,
        /** Frequency of the sinusoid. */
        val frequency: Double,
        /** Amplitude of the sinusoid. 0.0 < a <= 1.0 */
        val amplitude: Double,
        /** Number of samples to generate. */
        val time: Double,
        /** Phase of the sinusoid in radians. */
        val phase: Double = 0.0
) : AudioInput {

    private val samplesCount = (time * sampleRate).toInt()

    override fun info(): Map<String, String> {
        return mapOf(
                "Sinusoid amplitude" to "$amplitude",
                "Sinusoid phase" to "$phase",
                "Sinusoid frequency" to "${frequency}Hz"
        )
    }

    override fun size(): Int {
        return samplesCount
    }

    override fun subInput(skip: Int, length: Int): AudioInput {
        val newLength = length / sampleRate
        val samplesInPeriod = sampleRate / frequency
        val periodsToSkip = skip / samplesInPeriod
        return SineGeneratedInput(sampleRate, frequency, amplitude, newLength.toDouble(), phase + periodsToSkip * 2 * Math.PI)
    }

    override fun asSequence(): Sequence<Sample> {
        return object : Iterator<Sample> {

            private var x = 0.0
            private val delta = 1.0 / sampleRate

            override fun hasNext(): Boolean = x < time

            override fun next(): Sample {
                if (!hasNext()) NoSuchElementException("")
                val r = sampleOf(sin(x))
                x += delta
                return r
            }
        }.asSequence()
    }

    private fun sin(x: Double): Double =
            amplitude * cos(x * 2 * Math.PI * frequency + phase)

}