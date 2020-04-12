package io.wavebeans.execution.pod

import io.wavebeans.execution.medium.MediumConverter
import io.wavebeans.lib.BeanStream

class AnySplittingPod(
        bean: BeanStream<Any>,
        podKey: PodKey,
        partitionCount: Int
) : SplittingPod<Any, BeanStream<Any>>(
        bean = bean,
        podKey = podKey,
        partitionCount = partitionCount
)