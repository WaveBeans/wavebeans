package io.wavebeans.lib.stream.fft

import io.wavebeans.lib.*
import io.wavebeans.lib.math.ComplexNumber
import io.wavebeans.lib.math.r
import io.wavebeans.lib.stream.window.Window
import kotlinx.serialization.Serializable
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.log10

fun BeanStream<Window<Sample>>.fft(binCount: Int): BeanStream<FftSample> = FftStream(this, FftStreamParams(binCount))

data class FftSample(
        val time: Long,
        val binCount: Int,
        val sampleRate: Float,
        val fft: List<ComplexNumber>
) {

    fun magnitude(): Sequence<Double> = fft.asSequence()
            .take(binCount / 2)
            .map { 20 * log10(it.abs()) }

    fun phase(): Sequence<Double> = fft.asSequence()
            .take(binCount / 2)
            .map {
                val phi = it.phi()
                val doublePiCycles = (phi / (2 * PI)).toInt()
                phi - 2 * PI * doublePiCycles
            }

    fun frequency(): Sequence<Double> = (0 until (binCount / 2)).asSequence().map { it * sampleRate / binCount.toDouble() }
}


@Serializable
data class FftStreamParams(
        val n: Int,
        val start: Long = 0,
        val end: Long? = null,
        val timeUnit: TimeUnit = TimeUnit.MILLISECONDS
) : BeanParams()

class FftStream(
        override val input: BeanStream<Window<Sample>>,
        override val parameters: FftStreamParams
) : BeanStream<FftSample>, AlterBean<Window<Sample>, FftSample>, SinglePartitionBean {

    override fun asSequence(sampleRate: Float): Sequence<FftSample> {
        return input.asSequence(sampleRate)
                .mapIndexed { idx, window ->
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
                            time = (idx.toDouble() * m.toDouble() / (sampleRate.toDouble() / 1e+9)).toLong(),
                            binCount = parameters.n,
                            fft = fft.toList(),
                            sampleRate = sampleRate
                    )
                }
    }
}