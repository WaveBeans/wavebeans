package mux.lib.execution

import mux.lib.Bean
import mux.lib.BeanParams
import mux.lib.SampleArray
import mux.lib.io.StreamInput
import java.util.concurrent.TimeUnit

class StreamInputStreamingPod(
        val input: StreamInput,
        podKey: PodKey
) : StreamingPod<SampleArray, StreamInput>(podKey), StreamInput {
    override fun asSequence(sampleRate: Float): Sequence<SampleArray> = input.asSequence(sampleRate)

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): StreamInput {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override val parameters: BeanParams
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
}

class StreamInputSplittingPod(
        val input: StreamInput,
        podKey: PodKey,
        partitionCount: Int
) : SplittingPod<SampleArray, StreamInput>(podKey, partitionCount), StreamInput {
    override fun asSequence(sampleRate: Float): Sequence<SampleArray> = input.asSequence(sampleRate)

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): StreamInput {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override val parameters: BeanParams
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
}

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