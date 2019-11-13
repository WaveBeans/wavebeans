package mux.lib.execution

import mux.lib.Bean
import mux.lib.BeanParams
import mux.lib.SampleArray
import mux.lib.io.StreamInput
import java.util.concurrent.TimeUnit

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