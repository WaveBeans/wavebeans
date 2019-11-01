package mux.lib.execution

import mux.lib.*
import mux.lib.execution.TopologySerializer.jsonPretty
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.typeOf

@ExperimentalStdlibApi
fun Topology.buildPods(): List<AnyPod> = PodBuilder(this).build()


@ExperimentalStdlibApi
class PodBuilder(private val topology: Topology) {

    private val beansById = topology.refs.groupBy { it.id }

    private val beanLinks = topology.links.groupBy { it.from }

    fun build(): List<AnyPod> {
        val builtPods = mutableListOf<AnyPod>()

        val createdPods = mutableSetOf<PodKey>()
        for (beanRefs in beansById.values) {
            val ref = beanRefs.first()
            check(ref.id !in createdPods) { "Topology should already be created without duplication. Found duplicated bean: $ref" }
            createdPods += ref.id
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
                        val links = beanLinks.getValue(beanRef.id)
                                .filter { it.fromPartition == beanRef.partition }
                        if (links.size == 1) {
                            val link = links.first()
                            val podProxy = PodRegistry.createPodProxy(
                                    podProxyType,
                                    link.to,
                                    link.toPartition
                            )

                            PodRegistry.createPod(
                                    beanClazz.supertypes.first { it.isSubtypeOf(typeOf<Bean<*, *>>()) },
                                    beanRef.id,
                                    constructor.call(podProxy, beanRef.params) as Bean<*, *>,
                                    beanRef.partition
                            )
                        } else {
                            val podProxy = PodRegistry.createMergingPodProxy(
                                    podProxyType,
                                    links.map { Pair(it.to, it.toPartition) }
                            )

                            PodRegistry.createPod(
                                    beanClazz.supertypes.first { it.isSubtypeOf(typeOf<Bean<*, *>>()) },
                                    beanRef.id,
                                    constructor.call(podProxy, beanRef.params) as Bean<*, *>,
                                    beanRef.partition
                            )
                        }
                    }
                }

                beanClazz.isSubclassOf(SourceBean::class) -> {
                    val constructor = beanClazz.constructors.first {
                        it.parameters.size == 1 &&
                                it.parameters[0].type.isSubtypeOf(typeOf<BeanParams>())
                    }

                    beanRefs.map { beanRef ->
                        PodRegistry.createPod(
                                beanClazz.supertypes.first { it.isSubtypeOf(typeOf<Bean<*, *>>()) },
                                beanRef.id,
                                constructor.call(beanRef.params) as Bean<*, *>,
                                beanRef.partition
                        )
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
                        val links = beanLinks.getValue(beanRef.id)
                                .filter { it.fromPartition == beanRef.partition }
                        if (links.size == 2) {
                            throw UnsupportedOperationException("Non-merging pod proxy is not implemented.")
                        } else {
                            // requires merging first
                            val podProxy1 = PodRegistry.createMergingPodProxy(
                                    podProxyType1,
                                    links.filter { it.order == 0 }.map { Pair(it.to, it.toPartition) }
                            )
                            val podProxy2 = PodRegistry.createMergingPodProxy(
                                    podProxyType2,
                                    links.filter { it.order == 1 }.map { Pair(it.to, it.toPartition) }
                            )

                            PodRegistry.createPod(
                                    beanClazz.supertypes.first { it.isSubtypeOf(typeOf<Bean<*, *>>()) },
                                    beanRef.id,
                                    constructor.call(podProxy1, podProxy2, beanRef.params) as Bean<*, *>,
                                    beanRef.partition

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