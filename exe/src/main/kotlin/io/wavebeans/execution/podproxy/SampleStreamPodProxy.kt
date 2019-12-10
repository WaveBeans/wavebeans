package io.wavebeans.execution.podproxy

import io.wavebeans.execution.pod.PodKey
import io.wavebeans.execution.medium.SampleArray
import io.wavebeans.execution.medium.nullableSampleArrayList
import io.wavebeans.lib.BeanStream
import io.wavebeans.lib.Sample

class SampleStreamPodProxy(
        podKey: PodKey,
        forPartition: Int
) : BeanStream<Sample>, StreamingPodProxy<Sample, SampleArray>(
        pointedTo = podKey,
        forPartition = forPartition,
        converter = { it.nullableSampleArrayList() },
        elementExtractor = { arr, i -> if (i < arr.size) arr[i] else null }
)

class SampleStreamMergingPodProxy(
        override val readsFrom: List<PodKey>,
        forPartition: Int
) : MergingPodProxy<Sample, SampleArray>(
        forPartition = forPartition,
        converter = { it.nullableSampleArrayList() },
        elementExtractor = { arr, i -> if (i < arr.size) arr[i] else null }
), BeanStream<Sample>
