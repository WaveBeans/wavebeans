package io.wavebeans.lib.stream.fft

import io.wavebeans.lib.*
import io.wavebeans.lib.io.input
import io.wavebeans.lib.math.r
import io.wavebeans.lib.stream.AbstractOperationBeanStream
import io.wavebeans.lib.stream.merge
import io.wavebeans.lib.stream.window.Window
import kotlinx.serialization.Serializable

/**
 * Calculates the FFT of the specified windowed [Sample] stream. The [binCount] should be a power of 2, though the
 * underlying window might of less size, remaining elements will be zero-padded.
 *
 * To improve the output you may use [io.wavebeans.lib.stream.window.windowFunction] beforehand on the window stream.
 *
 * @param binCount number of bins in the FFT calculation, should be of power of 2 and greater or equal to underlying [Window.size].
 */
fun BeanStream<Window<Sample>>.fft(binCount: Int): BeanStream<FftSample> =
        FftStream(
                this.merge(input { it.first }) { (window, index) ->
                    requireNotNull(index)
                    window?.let { index to it }
                },
                FftStreamParams(binCount)
        )

/**
 * Parameters for [FftStream]
 */
@Serializable
data class FftStreamParams(
        /**
         * Number of bins in the FFT calculation, should be of power of 2 and greater or equal to underlying [Window.size]
         */
        val binCount: Int
) : BeanParams()

/**
 * Calculates the FFT of the specified windowed [Sample] stream. The [FftStreamParams.binCount] should be a power of 2, though the
 * underlying window might of less size, remaining elements will be zero-padded.
 *
 * To improve the output you may use [io.wavebeans.lib.stream.window.windowFunction] beforehand on the window stream.
 *
 * @param input the input stream of windowed [Sample]s to read from.
 * @param parameters tuning parameters, primarily [FftStreamParams.binCount].
 */
class FftStream(
        override val input: BeanStream<Pair<Long, Window<Sample>>>,
        override val parameters: FftStreamParams
) : AbstractOperationBeanStream<Pair<Long, Window<Sample>>, FftSample>(input), AlterBean<Pair<Long, Window<Sample>>, FftSample> {

    override fun operationSequence(input: Sequence<Pair<Long, Window<Sample>>>, sampleRate: Float): Sequence<FftSample> {
        return input
                .map { (index, window) ->
                    require(window.elements.size <= parameters.binCount) {
                        "The window size (${window.elements.size}) " +
                                "must be less or equal than N (${parameters.binCount})"
                    }
                    require(!(parameters.binCount == 0 || parameters.binCount and (parameters.binCount - 1) != 0)) {
                        "N should be power of 2 but ${parameters.binCount} found"
                    }
                    val m = window.elements.size
                    val fft = fft(
                            x = window.elements.asSequence()
                                    .map { it.r }
                                    .zeropad(m, parameters.binCount),
                            n = parameters.binCount
                    )

                    FftSample(
                            index = index,
                            binCount = parameters.binCount,
                            samplesCount = m,
                            samplesLength = window.step,
                            fft = fft.toList(),
                            sampleRate = sampleRate
                    )
                }
    }
}