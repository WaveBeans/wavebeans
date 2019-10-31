package mux.lib.execution

import mux.lib.Bean
import mux.lib.Sample
import mux.lib.SampleArray
import mux.lib.io.StreamInput
import mux.lib.io.StreamOutput
import mux.lib.stream.FiniteSampleStream
import mux.lib.stream.SampleStream
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.full.isSupertypeOf
import kotlin.reflect.typeOf

@ExperimentalStdlibApi
object PodRegistry {

    private val podProxyRegistry = mutableMapOf<KType, KFunction<AnyPodProxy>>()
    private val mergingPodProxyRegistry = mutableMapOf<KType, KFunction<MergingPodProxy<*, *>>>()
    private val podRegistry = mutableMapOf<KType, KFunction<AnyPod>>()

    init {
        registerPodProxy(typeOf<FiniteSampleStream>(), FiniteSampleStreamPodProxy::class.constructors.first())
        registerPodProxy(typeOf<SampleStream>(), SampleStreamPodProxy::class.constructors.first())
        registerPodProxy(typeOf<StreamInput>(), StreamInputPodProxy::class.constructors.first())
        registerMergingPodProxy(typeOf<FiniteSampleStream>(), FiniteSampleStreamMergingPodProxy::class.constructors.first())
        registerMergingPodProxy(typeOf<SampleStream>(), SampleStreamMergingPodProxy::class.constructors.first())
        registerMergingPodProxy(typeOf<StreamInput>(), StreamInputMergingPodProxy::class.constructors.first())
        registerPod(typeOf<FiniteSampleStream>(), FiniteSampleStreamPod::class.constructors.first())
        registerPod(typeOf<SampleStream>(), SampleStreamPod::class.constructors.first())
        registerPod(typeOf<StreamOutput<SampleArray, FiniteSampleStream>>(), SampleStreamOutputPod::class.constructors.first())
        registerPod(typeOf<StreamInput>(), StreamInputPod::class.constructors.first())
    }

    fun registerPodProxy(outputType: KType, constructor: KFunction<PodProxy<*, *>>) {
        podProxyRegistry[outputType] = constructor
    }

    fun registerMergingPodProxy(outputType: KType, constructor: KFunction<MergingPodProxy<*, *>>) {
        mergingPodProxyRegistry[outputType] = constructor
    }

    fun registerPod(inputType: KType, constructor: KFunction<Pod<*, *>>) {
        podRegistry[inputType] = constructor
    }

    fun createPodProxy(nodeType: KType, podKey: PodKey, partition: Int): AnyPodProxy =
            podProxyRegistry[findRegisteredType(nodeType, podProxyRegistry.keys)]?.call(podKey, partition)
                    ?: throw IllegalStateException("PodProxy for `$nodeType` is not found")

    fun createMergingPodProxy(nodeType: KType, podKeyAndPartition: List<Pair<PodKey, Int>>): AnyPodProxy =
            mergingPodProxyRegistry[findRegisteredType(nodeType, podProxyRegistry.keys)]?.call()
                    ?: throw IllegalStateException("PodProxy for `$nodeType` is not found")

    fun createPod(nodeType: KType, podKey: PodKey, node: Bean<*, *>, partition: Int): AnyPod =
            podRegistry[findRegisteredType(nodeType, podRegistry.keys)]?.call(node, podKey, partition)
                    ?: throw IllegalStateException("Pod for `$nodeType` is not found")

    private fun findRegisteredType(type: KType, registeredTypes: Set<KType>): KType? {
        if (type in registeredTypes) // if the direct key exists return it
            return type
        // otherwise try to find an approximation
        return registeredTypes.firstOrNull {
            it.isSupertypeOf(type)
        }
    }
}