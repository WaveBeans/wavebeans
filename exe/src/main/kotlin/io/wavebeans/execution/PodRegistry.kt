package io.wavebeans.execution

import io.wavebeans.execution.pod.*
import io.wavebeans.execution.podproxy.*
import io.wavebeans.lib.AnyBean
import io.wavebeans.lib.Bean
import io.wavebeans.lib.BeanStream
import io.wavebeans.lib.Sample
import io.wavebeans.lib.io.StreamInput
import io.wavebeans.lib.io.StreamOutput
import io.wavebeans.lib.stream.FiniteSampleStream
import io.wavebeans.lib.stream.fft.FftSample
import io.wavebeans.lib.stream.fft.FftStream
import io.wavebeans.lib.stream.fft.FiniteFftStream
import io.wavebeans.lib.stream.window.SampleWindowStream
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
        registerPodProxy(typeOf<SampleWindowStream>(), SampleWindowStreamPodProxy::class.constructors.first())
        registerPodProxy(typeOf<FiniteFftStream>(), FiniteFftStreamPodProxy::class.constructors.first())
        registerPodProxy(typeOf<BeanStream<Sample>>(), SampleStreamPodProxy::class.constructors.first())
        registerPodProxy(typeOf<BeanStream<*>>(), AnyStreamPodProxy::class.constructors.first())
        registerPodProxy(typeOf<FftStream>(), FftStreamPodProxy::class.constructors.first())

        registerMergingPodProxy(typeOf<FiniteSampleStream>(), FiniteFftStreamMergingPodProxy::class.constructors.first())
        registerMergingPodProxy(typeOf<SampleWindowStream>(), SampleWindowMergingPodProxy::class.constructors.first())
        registerMergingPodProxy(typeOf<FiniteFftStream>(), FiniteFftStreamMergingPodProxy::class.constructors.first())
        registerMergingPodProxy(typeOf<BeanStream<Sample>>(), SampleStreamMergingPodProxy::class.constructors.first())
        registerMergingPodProxy(typeOf<FftStream>(), FftStreamMergingPodProxy::class.constructors.first())

        registerPod(typeOf<BeanStream<Sample>>(), SampleStreamingPod::class.constructors.single { it.parameters.size == 2 })
        registerPod(typeOf<BeanStream<FftSample>>(), FftSampleStreamingPod::class.constructors.single { it.parameters.size == 2 })
        registerPod(typeOf<BeanStream<*>>(), AnyStreamingPod::class.constructors.single { it.parameters.size == 2 })
        registerPod(typeOf<StreamOutput<Sample>>(), SampleStreamOutputPod::class.constructors.first())
        registerPod(typeOf<StreamOutput<FftSample>>(), FftSampleStreamOutputPod::class.constructors.first())

        registerSplittingPod(typeOf<BeanStream<Sample>>(), SampleSplittingPod::class.constructors.single { it.parameters.size == 3 })
        registerSplittingPod(typeOf<WindowStream<Sample>>(), WindowSampleSplittingPod::class.constructors.single { it.parameters.size == 3 })
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