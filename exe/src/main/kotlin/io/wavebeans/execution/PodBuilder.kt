package io.wavebeans.execution

import io.wavebeans.execution.pod.PodKey
import io.wavebeans.lib.*
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.typeOf

@ExperimentalStdlibApi
fun Topology.buildPods(): List<PodRef> = PodBuilder(this).build()


@ExperimentalStdlibApi
class PodBuilder(val topology: Topology) {

    private val beansById = topology.refs.groupBy { it.id }

    private val beanLinksFrom = topology.links.groupBy { it.from }

    private val beanLinksTo = topology.links.groupBy { it.to }

    fun build(): List<PodRef> {
        return beansById.values.flatten().map { beanRef ->
            val beanClazz = WaveBeansClassLoader.classForName(beanRef.type).kotlin
            val (classForProxy, beanRefs, beanLinks) =
                    if (beanClazz.isSubclassOf(BeanGroup::class)) {
                        val groupParams = beanRef.params as BeanGroupParams
                        Triple(
                                WaveBeansClassLoader.classForName(groupParams.beanRefs.last().type).kotlin,
                                groupParams.beanRefs,
                                groupParams.links
                        )
                    } else {
                        Triple(beanClazz, listOf(beanRef), emptyList())
                    }

            val amountOfProvidedPartitions = beanLinksTo[beanRef.id]?.asSequence()
                    ?.filter { it.toPartition == beanRef.partition }
                    ?.map { it.fromPartition }
                    ?.distinct()
                    ?.count()
                    ?: 0
            val splitToPartitions = if (amountOfProvidedPartitions > 1) // if pod should provide data for more than 1 partition
                topology.partitionsCount // then pod will split up the stream
            else
                null // otherwise will stream to the same partition

            val proxies = createProxies(classForProxy, beanRef)

            PodRef(
                    PodKey(beanRef.id, beanRef.partition),
                    beanRefs,
                    beanLinks,
                    proxies,
                    splitToPartitions
            )

        }
    }

    private fun createProxies(classForProxy: KClass<out Any>, beanRef: BeanRef): List<PodProxyRef> {
        return when {
            // pod should have has 1 input
            classForProxy.isSubclassOf(SingleBean::class) || classForProxy.isSubclassOf(AlterBean::class) -> {

                val podProxyType = classForProxy.constructors.first {
                    it.parameters.size == 2 &&
                            it.parameters[0].type.isSubtypeOf(typeOf<AnyBean>()) &&
                            it.parameters[1].type.isSubtypeOf(typeOf<BeanParams>())
                }.parameters[0].type

                val links = beanLinksFrom[beanRef.id]
                        ?.filter { it.fromPartition == beanRef.partition }
                        ?: emptyList()
                val podProxy = PodProxyRef(podProxyType, links.map { PodKey(it.to, it.toPartition) }, beanRef.partition)

                listOf(podProxy)
            }

            // pod should have no inputs
            classForProxy.isSubclassOf(SourceBean::class) -> {
                emptyList()
            }

            // pod should have 2 inputs (or more?)
            classForProxy.isSubclassOf(MultiBean::class) || classForProxy.isSubclassOf(MultiAlterBean::class) -> {
                // TODO add support for 2+
                val constructor = classForProxy.constructors.first {
                    it.parameters.size == 3 &&
                            it.parameters[0].type.isSubtypeOf(typeOf<AnyBean>()) &&
                            it.parameters[1].type.isSubtypeOf(typeOf<AnyBean>()) &&
                            it.parameters[2].type.isSubtypeOf(typeOf<BeanParams>())
                }
                val podProxyType1 = constructor.parameters[0].type
                val podProxyType2 = constructor.parameters[1].type
                val links = beanLinksFrom.getValue(beanRef.id)
                        .filter { it.fromPartition == beanRef.partition }
                val podProxy1 = PodProxyRef(
                        podProxyType1,
                        links.filter { it.order == 0 }.map { PodKey(it.to, it.toPartition) },
                        beanRef.partition
                )
                val podProxy2 = PodProxyRef(
                        podProxyType2,
                        links.filter { it.order == 1 }.map { PodKey(it.to, it.toPartition) },
                        beanRef.partition
                )

                listOf(podProxy1, podProxy2)
            }

            else -> throw UnsupportedOperationException("Unsupported proxy class $classForProxy")

        }
    }
}