package mux.lib.stream

import mux.lib.Window
import mux.lib.fft
import mux.lib.math.r
import mux.lib.math.times
import mux.lib.zeropad
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.log10

class WindowFftStream(
        val sampleStream: SampleStream,
        val m: Int,
        val window: Window,
        val start: Long = 0,
        val end: Long? = null,
        val timeUnit: TimeUnit = TimeUnit.MILLISECONDS

) : FftStream {

    override fun estimateFftSamplesCount(samplesCount: Long): Long = samplesCount / m


    override fun asSequence(sampleRate: Float): Sequence<FftSample> {
        return sampleStream.asSequence(sampleRate)
                .map { it.r }
                .windowed(m, m, true)
                .mapIndexed { idx, fftWindow ->
                    val n = window.n
                    val fft = fft(
                            x = fftWindow.asSequence()
                                    .zeropad(m, n)
                                    .zip(window.asSequence())
                                    .map { it.first * it.second },
                            n = n
                    )
                    FftSample(
                            time = (idx * m / (sampleRate / 10e+9)).toLong(),
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