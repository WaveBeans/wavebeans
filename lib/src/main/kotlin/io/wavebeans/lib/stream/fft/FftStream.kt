package io.wavebeans.lib.stream.fft

import io.wavebeans.lib.*
import io.wavebeans.lib.math.r
import io.wavebeans.lib.stream.window.Window
import kotlinx.serialization.Serializable

fun BeanStream<Window<Sample>>.fft(binCount: Int): BeanStream<FftSample> = FftStream(this, FftStreamParams(binCount))

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
                            samplesLength = window.step,
                            fft = fft.toList(),
                            sampleRate = sampleRate
                    )
                }
    }
}