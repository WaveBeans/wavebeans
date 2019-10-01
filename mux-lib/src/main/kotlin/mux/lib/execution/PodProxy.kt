package mux.lib.execution

import mux.lib.Bean
import mux.lib.BeanParams
import mux.lib.BeanStream
import mux.lib.Sample
import mux.lib.io.StreamInput
import mux.lib.stream.FiniteSampleStream
import mux.lib.stream.SampleStream
import java.util.concurrent.TimeUnit

typealias PodKey = Int
typealias BushKey = Int
typealias AnyPod = Pod<*, *>

interface PodProxy<T : Any, S : Any> : Bean<T, S>

abstract class AbstractStreamPodProxy<S : Any>(val podKey: PodKey) : BeanStream<Sample, S>, PodProxy<Sample, S> {

    override fun asSequence(sampleRate: Float): Sequence<Sample> {
        val bush = PodDiscovery.bushFor(podKey).first()
        val caller = BushCaller(bush, podKey)
        val iteratorKey = MessageEncoder.toLong(caller.call("iteratorStart")
                ?: throw IllegalStateException("Can't start iterator"))

        return object : Iterator<Sample> {
            override fun hasNext(): Boolean = true

            override fun next(): Sample {
                return MessageEncoder.toSample(caller.call("iteratorNext?iteratorKey=$iteratorKey")
                        ?: throw IllegalStateException("Can't get next element from iterator"))
            }

        }.asSequence()

    }

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): S = throw UnsupportedOperationException("That's not required for PodProxy")

    override fun inputs(): List<Bean<*, *>> = throw UnsupportedOperationException("That's not required for PodProxy")

    override val parameters: BeanParams
        get() = throw UnsupportedOperationException("That's not required for PodProxy")
}

@ExperimentalStdlibApi
class FiniteSampleStreamPodProxy(podKey: PodKey) : AbstractStreamPodProxy<FiniteSampleStream>(podKey), FiniteSampleStream {

    override fun length(timeUnit: TimeUnit): Long {
        val bush = PodDiscovery.bushFor(podKey).first()
        val caller = BushCaller(bush, podKey)

        return MessageEncoder.toLong(
                caller.call("length?timeUnit=${timeUnit.name}")
                        ?: throw IllegalStateException("Can't make call to $podKey")
        )
    }
}

class SampleStreamPodProxy(podKey: PodKey) : SampleStream, AbstractStreamPodProxy<SampleStream>(podKey)

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
