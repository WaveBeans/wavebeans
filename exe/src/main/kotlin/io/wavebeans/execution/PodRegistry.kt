package io.wavebeans.execution

import io.wavebeans.execution.pod.*
import io.wavebeans.execution.podproxy.*
import io.wavebeans.lib.AnyBean
import io.wavebeans.lib.BeanStream
import io.wavebeans.lib.Sample
import io.wavebeans.lib.io.StreamOutput
import io.wavebeans.lib.stream.FiniteSampleStream
import io.wavebeans.lib.stream.fft.FftSample
import io.wavebeans.lib.stream.fft.FftStream
import io.wavebeans.lib.stream.window.WindowStream
import mu.KotlinLogging
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.full.isSupertypeOf
import kotlin.reflect.typeOf

@ExperimentalStdlibApi
object PodRegistry {

    private val log = KotlinLogging.logger { }

    private val podProxyRegistry = mutableMapOf<KType, KFunction<AnyPodProxy>>()
    private val mergingPodProxyRegistry = mutableMapOf<KType, KFunction<MergingPodProxy<*, *>>>()
    private val podRegistry = mutableMapOf<KType, KFunction<Pod>>()
    private val splittingPodRegistry = mutableMapOf<KType, KFunction<Pod>>()

    init {
        registerPodProxy(typeOf<FiniteSampleStream>(), FiniteSampleStreamPodProxy::class.constructors.first())
        registerPodProxy(typeOf<BeanStream<*>>(), AnyStreamPodProxy::class.constructors.first())

        registerMergingPodProxy(typeOf<BeanStream<*>>(), AnyStreamMergingPodProxy::class.constructors.first())

        registerPod(typeOf<BeanStream<*>>(), AnyStreamingPod::class.constructors.single { it.parameters.size == 2 })
        registerPod(typeOf<StreamOutput<*>>(), AnyStreamOutputPod::class.constructors.first())

        registerSplittingPod(typeOf<BeanStream<*>>(), AnySplittingPod::class.constructors.single { it.parameters.size == 3 })
    }

    fun registerPodProxy(outputType: KType, constructor: KFunction<PodProxy<*>>) {
        podProxyRegistry[outputType] = constructor
    }

    fun registerMergingPodProxy(outputType: KType, constructor: KFunction<MergingPodProxy<*, *>>) {
        mergingPodProxyRegistry[outputType] = constructor
    }

    fun registerPod(inputType: KType, constructor: KFunction<Pod>) {
        podRegistry[inputType] = constructor
    }

    fun registerSplittingPod(inputType: KType, constructor: KFunction<Pod>) {
        splittingPodRegistry[inputType] = constructor
    }

    fun createPodProxy(beanType: KType, podKey: PodKey, forPartition: Int): AnyPodProxy =
            podProxyRegistry[findRegisteredType(beanType, podProxyRegistry.keys)]?.call(podKey, forPartition)
                    ?: throw IllegalStateException("PodProxy for `$beanType` is not found")

    fun createMergingPodProxy(beanType: KType, podKeys: List<PodKey>, forPartition: Int): AnyPodProxy =
            mergingPodProxyRegistry[findRegisteredType(beanType, mergingPodProxyRegistry.keys)]?.call(podKeys, forPartition)
                    ?: throw IllegalStateException("PodProxy for `$beanType` is not found")

    fun createPod(beanType: KType, podKey: PodKey, bean: AnyBean): Pod =
            podRegistry[findRegisteredType(beanType, podRegistry.keys)]?.call(bean, podKey)
                    ?: throw IllegalStateException("Pod for `$beanType` is not found")

    fun createSplittingPod(beanType: KType, podKey: PodKey, bean: AnyBean, partitionCount: Int): Pod =
            splittingPodRegistry[findRegisteredType(beanType, splittingPodRegistry.keys)]?.call(bean, podKey, partitionCount)
                    ?: throw IllegalStateException("Pod for `$beanType` is not found")

    private fun findRegisteredType(type: KType, registeredTypes: Set<KType>): KType? {
        log.trace { "Searching type $type among $registeredTypes, type.arguments=${type.arguments}" }
        if (type in registeredTypes) // if the direct key exists return it
            return type
        // otherwise try to find an approximation
        return registeredTypes.firstOrNull {
            it.isSupertypeOf(type)
        }
    }

}