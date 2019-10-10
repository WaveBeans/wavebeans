package mux.lib.execution

import mux.lib.Bean
import mux.lib.BeanParams
import mux.lib.Sample
import mux.lib.SampleArray
import mux.lib.io.StreamInput
import java.util.concurrent.TimeUnit

class StreamInputPod(
        val node: StreamInput,
        podKey: PodKey
) : StreamingPod<SampleArray, StreamInput>(podKey), StreamInput, Pod<SampleArray, StreamInput> {
    override fun asSequence(sampleRate: Float): Sequence<SampleArray> = node.asSequence(sampleRate)

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): StreamInput {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override val parameters: BeanParams
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
}

class StreamInputPodProxy(podKey: PodKey) : StreamInput, StreamingPodProxy<StreamInput>(podKey) {

    override fun inputs(): List<Bean<*, *>> {
        return super<StreamingPodProxy>.inputs()
    }
}