package io.wavebeans.lib.stream.fft

import io.wavebeans.lib.math.ComplexNumber
import io.wavebeans.lib.stream.Measured
import kotlinx.serialization.Serializable
import kotlin.math.PI
import kotlin.math.log10
import kotlin.math.min
import kotlin.math.round

@Serializable
data class FftSample(
        /**
         * The index of the sample in the stream.
         */
        val index: Long,
        /**
         * Number of bins of this FFT calculations, i.e. 512, 1024
         */
        val binCount: Int,
        /**
         * Number of samples the FFT is calculated based on.
         */
        val samplesCount: Int,
        /**
         * The actual length of the sample it is built on. Calculated based on the underlying window.
         */
        val samplesLength: Int,
        /**
         * Sample rate which was used to calculate the FFT
         */
        val sampleRate: Float,
        /**
         * The list of [ComplexNumber]s which is calculated FFT. Use [magnitude] and [phase] methods to extract magnitude and phase respectively.
         */
        val fft: List<ComplexNumber>
) : Measured {

    override fun measure(): Int = samplesLength

    /**
     * Gets the magnitude values for this FFT calculation. It is returned in logarithmic scale, using only first half of the FFT.
     */
    fun magnitude(): Sequence<Double> = fft.asSequence()
            .take(binCount / 2)
            .map { 20 * log10(it.abs()) }

    /**
     * Gets the phase values for this FFT calculation. It returns only first half of the FFT.
     */
    fun phase(): Sequence<Double> = fft.asSequence()
            .take(binCount / 2)
            .map {
                val phi = it.phi()
                val doublePiCycles = (phi / (2 * PI)).toInt()
                phi - 2 * PI * doublePiCycles
            }

    /**
     * Returns the frequency values for each of the bins.
     */
    fun frequency(): Sequence<Double> = (0 until (binCount / 2)).asSequence().map { it * sampleRate / binCount.toDouble() }

    /**
     * Finds the corresponding bin of the closest frequency in the FFT.
     *
     * Calculated using formula: binIndex = round(freq / sampleRate * binCount)
     *
     * @return the bin number of desired frequency
     */
    fun bin(frequency: Double): Int = min(binCount, round(frequency / sampleRate * binCount).toInt())

    /**
     * The time marker of this sample, in nano seconds.
     */
    fun time(): Long = (index.toDouble() * samplesCount.toDouble() / (sampleRate.toDouble() / 1e+9)).toLong()
}