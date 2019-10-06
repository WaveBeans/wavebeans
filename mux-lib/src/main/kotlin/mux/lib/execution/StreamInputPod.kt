package mux.lib.execution

import mux.lib.BeanParams
import mux.lib.Sample
import mux.lib.io.StreamInput
import java.util.concurrent.TimeUnit

class StreamInputPod(val node: StreamInput) : StreamInput, Pod<Sample, StreamInput> {
    override fun asSequence(sampleRate: Float): Sequence<Sample> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): StreamInput {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override val parameters: BeanParams
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
}

class StreamInputPodProxy(podKey: PodKey) : StreamInput, PodProxy<Sample, StreamInput> {

    override fun asSequence(sampleRate: Float): Sequence<Sample> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): StreamInput {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override val parameters: BeanParams
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
}