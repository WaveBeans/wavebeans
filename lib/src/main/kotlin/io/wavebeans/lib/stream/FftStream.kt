package io.wavebeans.lib.stream

import io.wavebeans.lib.BeanStream

data class FftSample(
        val time: Long,
        val binCount: Int,
        val magnitude: Sequence<Double>,
        val phase: Sequence<Double>,
        val frequency: Sequence<Double>
)

interface FftStream : BeanStream<FftSample, FftStream> {

    /***
     * Estimate number of FFT samples will be produced based on source samples count.
     *
     * @param samplesCount source sample count to base estimation on.
     */
    fun estimateFftSamplesCount(samplesCount: Long): Long
}


