package io.wavebeans.lib.stream.fft

import io.wavebeans.lib.*
import io.wavebeans.lib.math.ComplexNumber
import io.wavebeans.lib.math.r
import io.wavebeans.lib.stream.window.Window
import kotlinx.serialization.Serializable
import kotlin.math.PI
import kotlin.math.log10
import kotlin.math.min
import kotlin.math.round

fun BeanStream<Window<Sample>>.fft(binCount: Int): BeanStream<FftSample> = FftStream(this, FftStreamParams(binCount))

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
         * Sample rate which was used to calculate the FFT
         */
        val sampleRate: Float,
        /**
         * The list of [ComplexNumber]s which is calculated FFT. Use [magnitude] and [phase] methods to extract magnitude and phase respectively.
         */
        val fft: List<ComplexNumber>
) {

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


@Serializable
data class FftStreamParams(
        val n: Int
) : BeanParams()

class FftStream(
        override val input: BeanStream<Window<Sample>>,
        override val parameters: FftStreamParams
) : BeanStream<FftSample>, AlterBean<Window<Sample>, FftSample>, SinglePartitionBean {

    override fun asSequence(sampleRate: Float): Sequence<FftSample> {
        var idx = 0L
        return input.asSequence(sampleRate)
                .map { window ->
                    require(window.elements.size <= parameters.n) {
                        "The window size (${window.elements.size}) " +
                                "must be less or equal than N (${parameters.n})"
                    }
                    require(!(parameters.n == 0 || parameters.n and (parameters.n - 1) != 0)) {
                        "N should be power of 2 but ${parameters.n} found"
                    }
                    val m = window.elements.size
                    val fft = fft(
                            x = window.elements.asSequence()
                                    .map { it.r }
                                    .zeropad(m, parameters.n),
                            n = parameters.n
                    )

                    FftSample(
                            index = idx++,
                            binCount = parameters.n,
                            samplesCount = m,
                            fft = fft.toList(),
                            sampleRate = sampleRate
                    )
                }
    }
}