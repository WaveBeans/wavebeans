package io.wavebeans.lib.stream.fft

import io.wavebeans.lib.*
import io.wavebeans.lib.stream.window.Window

fun BeanStream<FftSample>.inverseFft(): BeanStream<Window<Sample>> = InverseFftStream(this)

class InverseFftStream(
        override val input: BeanStream<FftSample>,
        override val parameters: NoParams = NoParams()
) : BeanStream<Window<Sample>>, AlterBean<FftSample, Window<Sample>>, SinglePartitionBean {

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