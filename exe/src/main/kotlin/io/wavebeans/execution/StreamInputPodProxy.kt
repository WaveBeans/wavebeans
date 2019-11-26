package io.wavebeans.execution

import io.wavebeans.lib.Bean
import io.wavebeans.lib.SampleArray
import io.wavebeans.lib.io.StreamInput

class StreamInputPodProxy(podKey: PodKey, forPartition: Int) : StreamInput, StreamingPodProxy<SampleArray, StreamInput>(
        pointedTo = podKey,
        forPartition = forPartition,
        converter = { it.nullableSampleArrayList() }
) {

    override fun inputs(): List<Bean<*, *>> {
        return super<StreamingPodProxy>.inputs()
    }
}

class StreamInputMergingPodProxy(
        override val readsFrom: List<PodKey>,
        forPartition: Int
) : MergingPodProxy<SampleArray, StreamInput>(
        forPartition = forPartition,
        converter = { it.nullableSampleArrayList() }
), StreamInput {

    override fun inputs(): List<Bean<*, *>> {
        return super<MergingPodProxy>.inputs()
    }
}