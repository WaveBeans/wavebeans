package mux.lib.stream

import mux.lib.TimeRangeProjectable
import mux.lib.Window
import java.util.concurrent.TimeUnit

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

interface FftStream : MuxStream<FftSample>, TimeRangeProjectable<FftStream> {

    /***
     * Estimate number of FFT samples will be produced based on source samples count.
     *
     * @param samplesCount source sample count to base estimation on.
     */
    fun estimateFftSamplesCount(samplesCount: Long): Long
}


