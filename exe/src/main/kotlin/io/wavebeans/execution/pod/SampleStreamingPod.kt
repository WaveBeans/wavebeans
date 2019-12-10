package io.wavebeans.execution.pod

import io.wavebeans.execution.medium.SampleArray
import io.wavebeans.execution.medium.createSampleArray
import io.wavebeans.lib.BeanStream
import io.wavebeans.lib.Sample

class SampleStreamingPod(
        bean: BeanStream<Sample>,
        podKey: PodKey
) : StreamingPod<Sample, SampleArray, BeanStream<Sample>>(
        bean = bean,
        podKey = podKey,
        converter = { list ->
            val i = list.iterator()
            createSampleArray(list.size) { i.next() }
        }
)