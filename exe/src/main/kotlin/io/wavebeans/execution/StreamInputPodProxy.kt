package io.wavebeans.execution

import io.wavebeans.lib.Bean
import io.wavebeans.lib.Sample
import io.wavebeans.lib.io.StreamInput

class StreamInputPodProxy(podKey: PodKey, forPartition: Int) : StreamInput, StreamingPodProxy<Sample, StreamInput>(
        pointedTo = podKey,
        forPartition = forPartition,
        converter = { it.nullableSampleList() }
) {

    override fun inputs(): List<Bean<*, *>> {
        return super<StreamingPodProxy>.inputs()
    }
}

class StreamInputMergingPodProxy(
        override val readsFrom: List<PodKey>,
        forPartition: Int
) : MergingPodProxy<Sample, StreamInput>(
        forPartition = forPartition,
        converter = { it.nullableSampleList() }
), StreamInput {

    override fun inputs(): List<Bean<*, *>> {
        return super<MergingPodProxy>.inputs()
    }
}