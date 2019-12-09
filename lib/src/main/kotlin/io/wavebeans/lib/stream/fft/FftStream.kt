package io.wavebeans.lib.stream.fft

import io.wavebeans.lib.*
import io.wavebeans.lib.math.ComplexNumber
import io.wavebeans.lib.math.r
import io.wavebeans.lib.stream.window.SampleWindowStream
import io.wavebeans.lib.stream.window.Window
import kotlinx.serialization.Serializable
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.log10

fun SampleWindowStream.fft(binCount: Int): FftStream = FftStreamImpl(this, FftStreamParams(binCount))

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


interface FftStream : BeanStream<FftSample, FftStream> {
    /***
     * Estimate number of FFT samples will be produced based on source samples count.
     *
     * @param samplesCount source sample count to base estimation on.
     */
    fun estimateFftSamplesCount(samplesCount: Long): Long
}

@Serializable
data class FftStreamParams(
        val n: Int,
        val start: Long = 0,
        val end: Long? = null,
        val timeUnit: TimeUnit = TimeUnit.MILLISECONDS
) : BeanParams()

class FftStreamImpl(
        val sampleStream: SampleWindowStream,
        val params: FftStreamParams
) : FftStream, AlterBean<Window<Sample>, SampleWindowStream, FftSample, FftStream> {

    override val parameters: BeanParams = params

    override val input: Bean<Window<Sample>, SampleWindowStream> = sampleStream

    override fun estimateFftSamplesCount(samplesCount: Long): Long = samplesCount / sampleStream.parameters.windowSize

    override fun asSequence(sampleRate: Float): Sequence<FftSample> {
        require(sampleStream.parameters.windowSize <= params.n) {
            "The window size (${sampleStream.parameters.windowSize}) " +
                    "must be less or equal than N (${params.n})"
        }
        require(!(params.n == 0 || params.n and (params.n - 1) != 0)) {
            "N should be power of 2 but ${params.n} found"
        }
        return sampleStream.asSequence(sampleRate)
                .mapIndexed { idx, fftWindow ->
                    val m = sampleStream.parameters.windowSize
                    val fft = fft(
                            x = fftWindow.elements.asSequence()
                                    .map { it.r }
                                    .zeropad(m, params.n),
                            n = params.n
                    )

                    FftSample(
                            time = (idx * m / (sampleRate / 1e+9)).toLong(),
                            binCount = params.n,
                            fft = fft.toList(),
                            sampleRate = sampleRate
                    )
                }
    }

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): FftStream {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}