package io.wavebeans.execution.podproxy

import io.wavebeans.execution.pod.PodKey
import io.wavebeans.lib.BeanStream

class AnyStreamPodProxy(
        podKey: PodKey,
        forPartition: Int
) : BeanStream<Any>, StreamingPodProxy(
        pointedTo = podKey,
        forPartition = forPartition
)

class AnyStreamMergingPodProxy(
        override val readsFrom: List<PodKey>,
        forPartition: Int
) : MergingPodProxy(
        forPartition = forPartition
), BeanStream<Any>
