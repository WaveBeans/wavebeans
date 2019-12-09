package io.wavebeans.execution.podproxy

import io.wavebeans.execution.pod.PodKey
import io.wavebeans.execution.medium.SampleArray
import io.wavebeans.execution.medium.nullableSampleArrayList
import io.wavebeans.lib.Bean
import io.wavebeans.lib.Sample
import io.wavebeans.lib.ZeroSample
import io.wavebeans.lib.io.StreamInput

class StreamInputPodProxy(
        podKey: PodKey,
        forPartition: Int
) : StreamInput, StreamingPodProxy<Sample, StreamInput, SampleArray>(
        pointedTo = podKey,
        forPartition = forPartition,
        converter = { it.nullableSampleArrayList() },
        elementExtractor = { arr, i -> if (i < arr.size) arr[i] else null }
) {

    override fun inputs(): List<Bean<*, *>> {
        return super<StreamingPodProxy>.inputs()
    }
}

class StreamInputMergingPodProxy(
        override val readsFrom: List<PodKey>,
        forPartition: Int
) : MergingPodProxy<Sample, StreamInput, SampleArray>(
        forPartition = forPartition,
        converter = { it.nullableSampleArrayList() },
        elementExtractor = { arr, i -> if (i < arr.size) arr[i] else null }
), StreamInput {

    override fun inputs(): List<Bean<*, *>> {
        return super<MergingPodProxy>.inputs()
    }
}