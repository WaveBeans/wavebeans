package io.wavebeans.lib.stream.fft

import io.wavebeans.lib.*
import io.wavebeans.lib.stream.window.Window

/**
 * Inverses the FFT to a stream of windowed [Sample]s. The [Window] has the same attributes (size and step)
 * was created with.
 */
fun BeanStream<FftSample>.inverseFft(): BeanStream<Window<Sample>> = InverseFftStream(this)

/**
 * Inverses the FFT to a stream of windowed [Sample]s. The [Window] has the same attributes (size and step)
 * was created with.
 *
 * @param the input stream of [FftSample]s to read the FFTs from.
 * @param parameters no parameters to tune, by default [NoParams].
 */
class InverseFftStream(
        override val input: BeanStream<FftSample>,
        override val parameters: NoParams = NoParams()
) : BeanStream<Window<Sample>>, AlterBean<FftSample, Window<Sample>> {

    override fun asSequence(sampleRate: Float): Sequence<Window<Sample>> {
        return input.asSequence(sampleRate)
                .map { fftSample ->
                    val n = fftSample.binCount
                    val ifft = fft(
                            x = fftSample.fft.asSequence(),
                            n = n,
                            inversed = true
                    )

                    val m = fftSample.samplesCount
                    val realFft = ifft.map { it.re }.toList()
                    val y = realFft.takeLast(m / 2) + realFft.take((m + 1) / 2) // undo the zero-padding
                    Window(m, fftSample.samplesLength, y) { ZeroSample }
                }
    }
}