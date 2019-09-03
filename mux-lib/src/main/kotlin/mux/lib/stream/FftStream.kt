package mux.lib.stream

import mux.lib.MuxStream

data class FftSample(
        val time: Long,
        val binCount: Int,
        val magnitude: Sequence<Double>,
        val phase: Sequence<Double>,
        val frequency: Sequence<Double>
)

interface FftStream : MuxStream<FftSample, FftStream> {

    /***
     * Estimate number of FFT samples will be produced based on source samples count.
     *
     * @param samplesCount source sample count to base estimation on.
     */
    fun estimateFftSamplesCount(samplesCount: Long): Long
}


