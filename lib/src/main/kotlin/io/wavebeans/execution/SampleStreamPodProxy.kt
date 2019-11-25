package io.wavebeans.execution

import io.wavebeans.lib.SampleArray
import io.wavebeans.lib.stream.SampleStream

class SampleStreamPodProxy(podKey: PodKey, forPartition: Int) : SampleStream, StreamingPodProxy<SampleArray, SampleStream>(
        pointedTo = podKey,
        forPartition = forPartition,
        converter = { it.nullableSampleArrayList() }
)

class SampleStreamMergingPodProxy(
        override val readsFrom: List<PodKey>,
        forPartition: Int
) : MergingPodProxy<SampleArray, SampleStream>(
        forPartition = forPartition,
        converter = { it.nullableSampleArrayList() }
), SampleStream