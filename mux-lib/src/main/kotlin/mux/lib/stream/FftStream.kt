package mux.lib.stream

import mux.lib.Sample
import mux.lib.TimeRangeProjectable
import mux.lib.Window
import mux.lib.math.ComplexNumber
import mux.lib.math.r

fun SampleStream.fft(m: Int, window: Window): FftStream {
    return WindowFftStream(this, m, window)
}

data class FftSample(
        val time: Long,
        val binCount: Int,
        val magnitude: Sequence<Double>,
        val phase: Sequence<Double>,
        val frequency: Sequence<Double>
)

interface FftStream : MuxStream<FftSample>, TimeRangeProjectable<FftStream>

fun Sequence<Sample>.asComplex(): Sequence<ComplexNumber> = this.map { it.r }


