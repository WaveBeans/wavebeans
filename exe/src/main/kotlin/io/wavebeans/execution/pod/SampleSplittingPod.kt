package io.wavebeans.execution.pod

import io.wavebeans.execution.medium.SampleArray
import io.wavebeans.execution.medium.createSampleArray
import io.wavebeans.lib.BeanStream
import io.wavebeans.lib.Sample

class SampleSplittingPod(
        bean: BeanStream<Sample, *>,
        podKey: PodKey,
        partitionCount: Int
) : SplittingPod<Sample, SampleArray, BeanStream<Sample, *>>(
        bean = bean,
        podKey = podKey,
        partitionCount = partitionCount,
        converter = { list ->
            val i = list.iterator()
            createSampleArray(list.size) { i.next() }
        }
)