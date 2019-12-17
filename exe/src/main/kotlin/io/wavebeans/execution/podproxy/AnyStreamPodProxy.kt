package io.wavebeans.execution.podproxy

import io.wavebeans.execution.pod.PodKey
import io.wavebeans.execution.medium.SampleArray
import io.wavebeans.execution.medium.nullableSampleArrayList
import io.wavebeans.lib.BeanStream
import io.wavebeans.lib.Sample

class AnyStreamPodProxy(
        podKey: PodKey,
        forPartition: Int
) : BeanStream<Any>, StreamingPodProxy<Any, Array<Any>>(
        pointedTo = podKey,
        forPartition = forPartition,
        converter = { it.nullableSampleArrayList() as List<Array<Any>> },
        elementExtractor = { arr, i -> if (i < arr.size) arr[i] else null }
)

class AnyStreamMergingPodProxy(
        override val readsFrom: List<PodKey>,
        forPartition: Int
) : MergingPodProxy<Any, Array<Any>>(
        forPartition = forPartition,
        converter = { it.nullableSampleArrayList() as List<Array<Any>> },
        elementExtractor = { arr, i -> if (i < arr.size) arr[i] else null }
), BeanStream<Any>
