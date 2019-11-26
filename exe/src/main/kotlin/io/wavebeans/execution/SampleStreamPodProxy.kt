package io.wavebeans.execution

import io.wavebeans.lib.Sample
import io.wavebeans.lib.stream.SampleStream

class SampleStreamPodProxy(podKey: PodKey, forPartition: Int) : SampleStream, StreamingPodProxy<Sample, SampleStream>(
        pointedTo = podKey,
        forPartition = forPartition,
        converter = { it.nullableSampleList() }
)

class SampleStreamMergingPodProxy(
        override val readsFrom: List<PodKey>,
        forPartition: Int
) : MergingPodProxy<Sample, SampleStream>(
        forPartition = forPartition,
        converter = { it.nullableSampleList() }
), SampleStream