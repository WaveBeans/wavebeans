package mux.lib.execution

import mux.lib.*
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.typeOf

@ExperimentalStdlibApi
fun Topology.buildPods(): List<AnyPod> = PodBuilder(this).build()


@ExperimentalStdlibApi
class PodBuilder(private val topology: Topology) {

    private val beansById = topology.refs.groupBy { it.id }

    private val beanLinksFrom = topology.links.groupBy { it.from }

    private val beanLinksTo = topology.links.groupBy { it.to }

    fun build(): List<AnyPod> {
        val builtPods = mutableListOf<AnyPod>()

        for (beanRefs in beansById.values) {
            val ref = beanRefs.first()
            val beanClazz = Class.forName(ref.type).kotlin

            val pods: List<AnyPod> = when {
                beanClazz.isSubclassOf(SingleBean::class) || beanClazz.isSubclassOf(AlterBean::class) -> {
                    val constructor = beanClazz.constructors.first {
                        it.parameters.size == 2 &&
                                it.parameters[0].type.isSubtypeOf(typeOf<Bean<*, *>>()) &&
                                it.parameters[1].type.isSubtypeOf(typeOf<BeanParams>())
                    }

                    val podProxyType = constructor.parameters[0].type

                    beanRefs.map { beanRef ->
                        val links = beanLinksFrom.getValue(beanRef.id)
                                .filter { it.fromPartition == beanRef.partition }
                        if (links.size == 1) {
                            val link = links.first()
                            val podProxy = PodRegistry.createPodProxy(
                                    podProxyType,
                                    PodKey(link.to, link.toPartition)
                            )

                            PodRegistry.createPod(
                                    beanClazz.supertypes.first { it.isSubtypeOf(typeOf<Bean<*, *>>()) },
                                    PodKey(beanRef.id, beanRef.partition),
                                    constructor.call(podProxy, beanRef.params) as Bean<*, *>
                            )
                        } else {
                            val podProxy = PodRegistry.createMergingPodProxy(
                                    podProxyType,
                                    links.map { PodKey(it.to, it.toPartition) }
                            )

                            if (beanClazz.isSubclassOf(SinglePartitionBean::class) && !beanClazz.isSubclassOf(SinkBean::class)) {
                                PodRegistry.createSplittingPod(
                                        beanClazz.supertypes.first { it.isSubtypeOf(typeOf<Bean<*, *>>()) },
                                        PodKey(beanRef.id, beanRef.partition),
                                        constructor.call(podProxy, beanRef.params) as Bean<*, *>,
                                        beanLinksTo.getValue(beanRef.id).count()
                                )
                            } else {
                                PodRegistry.createPod(
                                        beanClazz.supertypes.first { it.isSubtypeOf(typeOf<Bean<*, *>>()) },
                                        PodKey(beanRef.id, beanRef.partition),
                                        constructor.call(podProxy, beanRef.params) as Bean<*, *>
                                )
                            }
                        }
                    }
                }

                beanClazz.isSubclassOf(SourceBean::class) -> {
                    val constructor = beanClazz.constructors.first {
                        it.parameters.size == 1 &&
                                it.parameters[0].type.isSubtypeOf(typeOf<BeanParams>())
                    }

                    beanRefs.map { beanRef ->

                        if (beanClazz.isSubclassOf(SinglePartitionBean::class)) {

                            PodRegistry.createSplittingPod(
                                    beanClazz.supertypes.first { it.isSubtypeOf(typeOf<Bean<*, *>>()) },
                                    PodKey(beanRef.id, beanRef.partition),
                                    constructor.call(beanRef.params) as Bean<*, *>,
                                    beanLinksTo.getValue(beanRef.id).count()
                            )
                        } else {
                            PodRegistry.createPod(
                                    beanClazz.supertypes.first { it.isSubtypeOf(typeOf<Bean<*, *>>()) },
                                    PodKey(beanRef.id, beanRef.partition),
                                    constructor.call(beanRef.params) as Bean<*, *>
                            )
                        }
                    }
                }

                beanClazz.isSubclassOf(MultiBean::class) -> {
                    // TODO add support for 2+
                    val constructor = beanClazz.constructors.first {
                        it.parameters.size == 3 &&
                                it.parameters[0].type.isSubtypeOf(typeOf<Bean<*, *>>()) &&
                                it.parameters[1].type.isSubtypeOf(typeOf<Bean<*, *>>()) &&
                                it.parameters[2].type.isSubtypeOf(typeOf<BeanParams>())
                    }
                    beanRefs.map { beanRef ->
                        val podProxyType1 = constructor.parameters[0].type
                        val podProxyType2 = constructor.parameters[1].type
                        val links = beanLinksFrom.getValue(beanRef.id)
                                .filter { it.fromPartition == beanRef.partition }
                        if (links.size == 2) {
                            throw UnsupportedOperationException("Non-merging pod proxy is not implemented.")
                        } else {
                            // requires merging first
                            val podProxy1 = PodRegistry.createMergingPodProxy(
                                    podProxyType1,
                                    links.filter { it.order == 0 }.map { PodKey(it.to, it.toPartition) }
                            )
                            val podProxy2 = PodRegistry.createMergingPodProxy(
                                    podProxyType2,
                                    links.filter { it.order == 1 }.map { PodKey(it.to, it.toPartition) }
                            )

                            PodRegistry.createSplittingPod(
                                    beanClazz.supertypes.first { it.isSubtypeOf(typeOf<Bean<*, *>>()) },
                                    PodKey(beanRef.id, beanRef.partition),
                                    constructor.call(podProxy1, podProxy2, beanRef.params) as Bean<*, *>,
                                    beanLinksTo.getValue(beanRef.id).count()
                            )
                        }
                    }
                }

                else -> throw UnsupportedOperationException("Unsupported class $beanClazz for decorating")
            }

            println("""
                BEAN CLASS: $beanClazz
                #: ${ref.id}
                POD: $pods
                POD INPUTS: ${pods.map { it.inputs() }.flatten()}
                POD INPUT INPUTS: ${pods.map { it.inputs() }.flatten().map { it.inputs() }.flatten()}
                -----
                """.trimIndent())

            builtPods += pods
        }

        return builtPods
    }
}