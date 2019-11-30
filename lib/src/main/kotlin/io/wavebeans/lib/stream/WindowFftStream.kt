package io.wavebeans.lib.stream

import io.wavebeans.lib.*
import io.wavebeans.lib.math.r
import io.wavebeans.lib.math.times
import kotlinx.serialization.Serializable
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.log10

fun SampleStream.fft(m: Int, window: DeprecatedWindow): FftStream = WindowFftStream(this, WindowFftStreamParams(m, window))

@Serializable
data class WindowFftStreamParams(
        val m: Int,
        val window: DeprecatedWindow,
        val start: Long = 0,
        val end: Long? = null,
        val timeUnit: TimeUnit = TimeUnit.MILLISECONDS
) : BeanParams()

class WindowFftStream(
        val sampleStream: SampleStream,
        val params: WindowFftStreamParams
) : FftStream, AlterBean<Sample, SampleStream, FftSample, FftStream> {

    override val parameters: BeanParams = params

    override val input: Bean<Sample, SampleStream> = sampleStream

    override fun estimateFftSamplesCount(samplesCount: Long): Long = samplesCount / params.m

    override fun asSequence(sampleRate: Float): Sequence<FftSample> {
        return sampleStream.asSequence(sampleRate)
                .map { it.r }
                .windowed(params.m, params.m, true)
                .mapIndexed { idx, fftWindow ->
                    val n = params.window.n
                    val fft = fft(
                            x = fftWindow.asSequence()
                                    .zeropad(params.m, n)
                                    .zip(params.window.asSequence())
                                    .map { it.first * it.second },
                            n = n
                    )
                    FftSample(
                            time = (idx * params.m / (sampleRate / 10e+9)).toLong(),
                            binCount = n,
                            magnitude = fft.take(n / 2).map { 20 * log10(it.abs()) },
                            phase = fft.take(n / 2).map {
                                val phi = it.phi()
                                val doublePiCycles = (phi / (2 * PI)).toInt()
                                phi - 2 * PI * doublePiCycles
                            },
                            frequency = (0 until (n / 2)).asSequence().map { it * sampleRate / n.toDouble() }
                    )
                }
    }

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): FftStream {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}