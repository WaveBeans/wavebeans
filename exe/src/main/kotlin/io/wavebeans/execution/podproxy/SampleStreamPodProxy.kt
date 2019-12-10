package io.wavebeans.execution.podproxy

import io.wavebeans.execution.pod.PodKey
import io.wavebeans.execution.medium.SampleArray
import io.wavebeans.execution.medium.nullableSampleArrayList
import io.wavebeans.lib.Sample
import io.wavebeans.lib.ZeroSample
import io.wavebeans.lib.stream.SampleStream

class SampleStreamPodProxy(
        podKey: PodKey,
        forPartition: Int
) : SampleStream, StreamingPodProxy<Sample, SampleStream, SampleArray>(
        pointedTo = podKey,
        forPartition = forPartition,
        converter = { it.nullableSampleArrayList() },
        elementExtractor = { arr, i -> if (i < arr.size) arr[i] else null }
)

class SampleStreamMergingPodProxy(
        override val readsFrom: List<PodKey>,
        forPartition: Int
) : MergingPodProxy<Sample, SampleStream, SampleArray>(
        forPartition = forPartition,
        converter = { it.nullableSampleArrayList() },
        elementExtractor = { arr, i -> if (i < arr.size) arr[i] else null }
), SampleStream