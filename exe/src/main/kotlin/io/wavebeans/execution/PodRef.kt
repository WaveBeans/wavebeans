package io.wavebeans.execution

import io.wavebeans.execution.pod.Pod
import io.wavebeans.execution.pod.PodKey
import io.wavebeans.lib.*
import kotlinx.serialization.*
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.typeOf

/**
 * Class encapsulates all the information needed to create a [Pod] of the specific type.
 * The class remains serializable in order to be transferred and/or persisted.
 */
@Serializable
data class PodRef(
        /**
         * The key the pod should appear under.
         */
        val key: PodKey,
        /**
         * beans to be inside of this pod. The first one is to be initialized with the proxies,
         * the last one is to be read from, the rest to be connected according to the [internalLinks].
         */
        val internalBeans: List<BeanRef>,
        /**
         * How internal links are interconnected between each other. Empty if only one bean in a pod.
         */
        val internalLinks: List<BeanLink>,
        /**
         * A list of [PodProxy] to be applied as the stream of the first bean from [internalBeans].
         * The order of the proxies is preserved and it will be matched during [io.wavebeans.lib.Bean] creation.
         * Currently supported 1 and 2 pod proxies per bean.
         */
        val podProxies: List<PodProxyRef>,
        /**
         * If more than one then assuming that the pod should split data among several partitions. If null then not applicable
         */
        val splitToPartitions: Int? = null
) {
    fun instantiate(sampleRate: Float): Pod {
        try {
            val proxies = podProxies.map {
                if (it.pointedTo.size > 1) {
                    PodRegistry.createMergingPodProxy(
                            it.type,
                            it.pointedTo,
                            it.partition
                    )
                } else {
                    PodRegistry.createPodProxy(
                            it.type,
                            it.pointedTo.single(),
                            it.partition
                    )

                }
            }

            // TODO follow internal links but not the order of the beans in the list
            // assumption: currently only first bean may have multiple inputs. Beans should be planned this way.
            fun createBean(inputs: List<AnyBean>, leftRefs: List<BeanRef>): AnyBean {
                if (leftRefs.isEmpty()) return inputs.single()
                val beanRef = leftRefs.first()
                try {
                    val beanClazz = WaveBeansClassLoader.classForName(beanRef.type).kotlin
                    val bean = when {
                        inputs.isEmpty() -> beanClazz.constructors.first {
                            it.parameters.size == 1 &&
                                    it.parameters[0].type.isSubtypeOf(typeOf<BeanParams>())
                        }.call(beanRef.params) as AnyBean
                        inputs.size == 1 -> beanClazz.constructors.first {
                            it.parameters.size == 2 &&
                                    it.parameters[0].type.isSubtypeOf(typeOf<AnyBean>()) &&
                                    it.parameters[1].type.isSubtypeOf(typeOf<BeanParams>())
                        }.call(inputs[0], beanRef.params) as AnyBean
                        inputs.size == 2 -> beanClazz.constructors.first {
                            it.parameters.size == 3 &&
                                    it.parameters[0].type.isSubtypeOf(typeOf<AnyBean>()) &&
                                    it.parameters[1].type.isSubtypeOf(typeOf<AnyBean>()) &&
                                    it.parameters[2].type.isSubtypeOf(typeOf<BeanParams>())
                        }.call(inputs[0], inputs[1], beanRef.params) as AnyBean
                        else -> throw UnsupportedOperationException("Too much input for the bean: ${inputs.size}")
                    }

                    return createBean(listOf(bean), leftRefs.drop(1))
                } catch (e: Exception) {
                    throw IllegalStateException("Can't create bean instance with inputs=$inputs, beanRef=$beanRef", e)
                }
            }

            val tailBean = createBean(proxies, internalBeans.reversed())
            val beanType = tailBean::class.createType(tailBean::class.typeParameters.map { KTypeProjection.STAR })
            return if (splitToPartitions == null) {
                if (tailBean is SinkBean<*>) {
                    PodRegistry.createPod(beanType, key, sampleRate, tailBean)
                } else {
                    PodRegistry.createPod(beanType, key, tailBean)
                }
            } else {
                PodRegistry.createSplittingPod(beanType, key, tailBean, splitToPartitions)
            }
        } catch (e: Exception) {
            throw IllegalStateException("Can't instantiate Pod=$this", e)
        }
    }

}

/**
 * Class encapsulates all the information required to create an instance of [PodProxy] of specific type.
 * The class remains serializable in order to be transferred and/or persisted.
 */
@Serializable
data class PodProxyRef(
        /**
         * [PodProxy] type to create it using [PodRegistry]
         */
        @Serializable(with = KTypeSerializer::class)
        val type: KType,
        /**
         * A list of PodKeys to make API calls to. If multiple elements, then it is a merging pod proxy that reads from multiple Pod at once.
         */
        val pointedTo: List<PodKey>,
        /**
         * Partition that is Pod is in. Pod handles only one partition.
         */
        val partition: Int
)

