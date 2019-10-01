package mux.lib.execution

import mux.lib.Bean
import mux.lib.BeanParams
import mux.lib.Sample
import mux.lib.io.StreamInput
import mux.lib.io.StreamOutput
import mux.lib.io.Writer
import mux.lib.stream.FiniteSampleStream
import mux.lib.stream.SampleStream
import java.util.concurrent.TimeUnit
import kotlin.reflect.KCallable

interface Pod<T : Any, S : Any> : Bean<T, S> {
    fun call(method: KCallable<*>, params: Array<Any?>): ByteArray?
}

class FiniteSampleStreamPod(val node: FiniteSampleStream) : FiniteSampleStream, Pod<Sample, FiniteSampleStream> {

    override fun call(method: KCallable<*>, params: Array<Any?>): ByteArray? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun length(timeUnit: TimeUnit): Long {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun asSequence(sampleRate: Float): Sequence<Sample> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): FiniteSampleStream {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun inputs(): List<Bean<*, *>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override val parameters: BeanParams
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

}

class SampleStreamPod(val node: SampleStream) : SampleStream, Pod<Sample, SampleStream> {
    override val parameters: BeanParams
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override fun call(method: KCallable<*>, params: Array<Any?>): ByteArray? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun asSequence(sampleRate: Float): Sequence<Sample> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): SampleStream {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun inputs(): List<Bean<*, *>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


}

class SampleStreamOutputPod(val node: StreamOutput<*, *>) : StreamOutput<Sample, FiniteSampleStream>, Pod<Sample, FiniteSampleStream> {
    override fun call(method: KCallable<*>, params: Array<Any?>): ByteArray? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun writer(sampleRate: Float): Writer {
        return node.writer(sampleRate)
    }

    override fun close() {
        node.close()
    }

    override val parameters: BeanParams
        get() = throw UnsupportedOperationException("PodProxy doesn't need it")

    override val input: Bean<Sample, FiniteSampleStream>
        get() = throw UnsupportedOperationException("PodProxy doesn't need it")
}

class StreamInputPod(val node: StreamInput) : StreamInput, Pod<Sample, StreamInput> {
    override fun call(method: KCallable<*>, params: Array<Any?>): ByteArray? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun asSequence(sampleRate: Float): Sequence<Sample> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): StreamInput {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override val parameters: BeanParams
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
}

