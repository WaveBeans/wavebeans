package mux.lib.execution

import mux.lib.Bean
import mux.lib.BeanParams
import mux.lib.Sample
import mux.lib.BeanStream
import mux.lib.io.StreamInput
import mux.lib.io.StreamOutput
import mux.lib.io.Writer
import mux.lib.stream.FiniteSampleStream
import mux.lib.stream.SampleStream
import java.util.concurrent.TimeUnit
import kotlin.reflect.KCallable
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.full.isSupertypeOf
import kotlin.reflect.typeOf

typealias PodKey = Int
typealias BushKey = Int
typealias AnyPodEndpoint = PodEndpoint<*, *>

interface Pod<T : Any, S : Any> : Bean<T, S>

interface PodEndpoint<T : Any, S : Any> : Bean<T, S> {
    fun call(endpointMethod: KCallable<*>, params: Array<Any?>): ByteArray?
}

@ExperimentalStdlibApi
object PodRegistry {

    private val podRegistry = mutableMapOf<KType, KFunction<Pod<*, *>>>()
    private val podEndpointRegistry = mutableMapOf<KType, KFunction<PodEndpoint<*, *>>>()

    init {
        registerPod(typeOf<FiniteSampleStream>(), FiniteSampleStreamPod::class.constructors.first())
        registerPod(typeOf<SampleStream>(), SampleStreamPod::class.constructors.first())
        registerPod(typeOf<StreamInput>(), StreamInputPod::class.constructors.first())
        registerPodEndpoint(typeOf<FiniteSampleStream>(), FiniteSampleStreamPodEndpoint::class.constructors.first())
        registerPodEndpoint(typeOf<SampleStream>(), SampleStreamPodEndpoint::class.constructors.first())
        registerPodEndpoint(typeOf<StreamOutput<Sample, FiniteSampleStream>>(), SampleStreamOutputPodEndpoint::class.constructors.first())
        registerPodEndpoint(typeOf<StreamInput>(), StreamInputPodEndpoint::class.constructors.first())
    }

    fun registerPod(outputType: KType, constructor: KFunction<Pod<*, *>>) {
        podRegistry[outputType] = constructor
    }

    fun registerPodEndpoint(inputType: KType, constructor: KFunction<PodEndpoint<*, *>>) {
        podEndpointRegistry[inputType] = constructor
    }

    fun createPod(nodeType: KType, podEndpointKey: Int): Pod<*, *> =
            podRegistry[findRegisteredKey(nodeType, podRegistry.keys)]?.call(podEndpointKey)
                    ?: throw IllegalStateException("Pod for `$nodeType` is not found")

    fun createPodEndpoint(nodeType: KType, node: Bean<*, *>): PodEndpoint<*, *> =
            podEndpointRegistry[findRegisteredKey(nodeType, podEndpointRegistry.keys)]?.call(node)
                    ?: throw IllegalStateException("PodEndpoint for `$nodeType` is not found")

    private fun findRegisteredKey(key: KType, keys: Set<KType>): KType? {
        if (key in keys) // if the direct key exists return it
            return key
        // otherwise try to find an approximation
        return keys.firstOrNull {
            println("$it : [$key] ? ${it.isSupertypeOf(key)}")
            it.isSupertypeOf(key)
        }
    }
}

abstract class AbstractStreamPod<S : Any>(val podEndpointKey: PodKey) : BeanStream<Sample, S>, Pod<Sample, S> {

    override fun asSequence(sampleRate: Float): Sequence<Sample> {
        val bush = PodDiscovery.bushFor(podEndpointKey).first()
        val caller = BushCaller(bush, podEndpointKey)
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

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): S = throw UnsupportedOperationException("That's not required for Pod")

    override fun inputs(): List<Bean<*, *>> = throw UnsupportedOperationException("That's not required for Pod")

    override val parameters: BeanParams
        get() = throw UnsupportedOperationException("That's not required for Pod")
}

@ExperimentalStdlibApi
class FiniteSampleStreamPod(podEndpointKey: Int) : AbstractStreamPod<FiniteSampleStream>(podEndpointKey), FiniteSampleStream {

    override fun length(timeUnit: TimeUnit): Long {
        val bush = PodDiscovery.bushFor(podEndpointKey).first()
        val caller = BushCaller(bush, podEndpointKey)

        return MessageEncoder.toLong(
                caller.call("length?timeUnit=${timeUnit.name}")
                        ?: throw IllegalStateException("Can't make call to $podEndpointKey")
        )
    }
}

class SampleStreamPod(podEndpointKey: Int) : SampleStream, AbstractStreamPod<SampleStream>(podEndpointKey)

class StreamInputPod(val sourceNodeId: Int) : StreamInput, Pod<Sample, StreamInput> {

    override fun asSequence(sampleRate: Float): Sequence<Sample> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): StreamInput {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override val parameters: BeanParams
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
}

class FiniteSampleStreamPodEndpoint(val node: FiniteSampleStream) : FiniteSampleStream, PodEndpoint<Sample, FiniteSampleStream> {

    override fun call(endpointMethod: KCallable<*>, params: Array<Any?>): ByteArray? {
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

class SampleStreamPodEndpoint(val node: SampleStream) : SampleStream, PodEndpoint<Sample, SampleStream> {
    override val parameters: BeanParams
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override fun call(endpointMethod: KCallable<*>, params: Array<Any?>): ByteArray? {
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

class SampleStreamOutputPodEndpoint(val node: StreamOutput<*, *>) : StreamOutput<Sample, FiniteSampleStream>, PodEndpoint<Sample, FiniteSampleStream> {
    override fun call(endpointMethod: KCallable<*>, params: Array<Any?>): ByteArray? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun writer(sampleRate: Float): Writer {
        return node.writer(sampleRate)
    }

    override fun close() {
        node.close()
    }

    override val parameters: BeanParams
        get() = throw UnsupportedOperationException("Pod doesn't need it")

    override val input: Bean<Sample, FiniteSampleStream>
        get() = throw UnsupportedOperationException("Pod doesn't need it")
}

class StreamInputPodEndpoint(val node: StreamInput) : StreamInput, PodEndpoint<Sample, StreamInput> {
    override fun call(endpointMethod: KCallable<*>, params: Array<Any?>): ByteArray? {
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

