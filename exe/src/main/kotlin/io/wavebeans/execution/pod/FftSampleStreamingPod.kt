package io.wavebeans.execution.pod

import io.wavebeans.execution.medium.FftSampleArray
import io.wavebeans.execution.medium.createFftSampleArray
import io.wavebeans.lib.BeanStream
import io.wavebeans.lib.stream.fft.FftSample
import io.wavebeans.lib.stream.fft.FftStream

class FftSampleStreamingPod(
        bean: BeanStream<FftSample>,
        podKey: PodKey
) : StreamingPod<FftSample, FftSampleArray, BeanStream<FftSample>>(
        bean = bean,
        podKey = podKey,
        converter = { list ->
            val i = list.iterator()
            createFftSampleArray(list.size) { i.next() }
        }
) {

    @Suppress("unused") // called via reflection
    fun estimateFftSamplesCount(samplesCount: Long): Long =
            if (bean is FftStream) bean.estimateFftSamplesCount(samplesCount)
            else throw UnsupportedOperationException("Not implemented for this bean type: ${this::class}")
}