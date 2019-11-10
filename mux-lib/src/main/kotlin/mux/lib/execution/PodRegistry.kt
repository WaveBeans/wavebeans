package mux.lib.execution

import mux.lib.Bean
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
    private val splittingPodRegistry = mutableMapOf<KType, KFunction<SplittingPod<*, *>>>()

    init {
        // TODO replace with annotations on classes themselves
        registerPodProxy(typeOf<FiniteSampleStream>(), FiniteSampleStreamPodProxy::class.constructors.first())
        registerPodProxy(typeOf<SampleStream>(), SampleStreamPodProxy::class.constructors.first())
        registerPodProxy(typeOf<StreamInput>(), StreamInputPodProxy::class.constructors.first())

        registerMergingPodProxy(typeOf<FiniteSampleStream>(), FiniteSampleStreamMergingPodProxy::class.constructors.first())
        registerMergingPodProxy(typeOf<SampleStream>(), SampleStreamMergingPodProxy::class.constructors.first())
        registerMergingPodProxy(typeOf<StreamInput>(), StreamInputMergingPodProxy::class.constructors.first())

        registerPod(typeOf<FiniteSampleStream>(), FiniteSampleStreamStreamingPod::class.constructors.first())
        registerPod(typeOf<SampleStream>(), SampleStreamStreamingPod::class.constructors.first())
        registerPod(typeOf<StreamOutput<SampleArray, FiniteSampleStream>>(), SampleStreamOutputPod::class.constructors.first())
        registerPod(typeOf<StreamInput>(), StreamInputStreamingPod::class.constructors.first())

        registerSplittingPod(typeOf<FiniteSampleStream>(), FiniteSampleStreamSplittingPod::class.constructors.first())
        registerSplittingPod(typeOf<SampleStream>(), SampleStreamSplittingPod::class.constructors.first())
        registerSplittingPod(typeOf<StreamInput>(), StreamInputSplittingPod::class.constructors.first())
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

    fun registerSplittingPod(inputType: KType, constructor: KFunction<SplittingPod<*, *>>) {
        splittingPodRegistry[inputType] = constructor
    }

    fun createPodProxy(nodeType: KType, podKey: PodKey): AnyPodProxy =
            podProxyRegistry[findRegisteredType(nodeType, podProxyRegistry.keys)]?.call(podKey)
                    ?: throw IllegalStateException("PodProxy for `$nodeType` is not found")

    fun createMergingPodProxy(beanType: KType, podKeys: List<PodKey>): AnyPodProxy =
            mergingPodProxyRegistry[findRegisteredType(beanType, mergingPodProxyRegistry.keys)]?.call(podKeys)
                    ?: throw IllegalStateException("PodProxy for `$beanType` is not found")

    fun createPod(beanType: KType, podKey: PodKey, bean: Bean<*, *>): AnyPod =
            podRegistry[findRegisteredType(beanType, podRegistry.keys)]?.call(bean, podKey)
                    ?: throw IllegalStateException("Pod for `$beanType` is not found")

    fun createSplittingPod(beanType: KType, podKey: PodKey, bean: Bean<*, *>, partitionCount: Int): AnyPod =
            splittingPodRegistry[findRegisteredType(beanType, splittingPodRegistry.keys)]?.call(bean, podKey, partitionCount)
                    ?: throw IllegalStateException("Pod for `$beanType` is not found")

    private fun findRegisteredType(type: KType, registeredTypes: Set<KType>): KType? {
        if (type in registeredTypes) // if the direct key exists return it
            return type
        // otherwise try to find an approximation
        return registeredTypes.firstOrNull {
            it.isSupertypeOf(type)
        }
    }

}