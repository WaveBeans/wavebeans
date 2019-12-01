package io.wavebeans.execution.pod

import io.wavebeans.execution.medium.FftSampleArray
import io.wavebeans.execution.medium.SampleArray
import io.wavebeans.execution.medium.createFftSampleArray
import io.wavebeans.execution.medium.createSampleArray
import io.wavebeans.lib.BeanStream
import io.wavebeans.lib.Sample
import io.wavebeans.lib.stream.fft.FftSample

class FftSampleStreamingPod(
        bean: BeanStream<FftSample, *>,
        podKey: PodKey
) : StreamingPod<FftSample, FftSampleArray, BeanStream<FftSample, *>>(
        bean = bean,
        podKey = podKey,
        converter = { list ->
            val i = list.iterator()
            createFftSampleArray(list.size) { i.next() }
        }
)