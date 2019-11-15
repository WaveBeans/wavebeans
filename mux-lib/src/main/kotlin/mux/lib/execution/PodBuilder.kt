package mux.lib.execution

import mux.lib.*
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.typeOf

@ExperimentalStdlibApi
fun Topology.buildPods(): List<PodRef> = PodBuilder(this).build()


@ExperimentalStdlibApi
class PodBuilder(topology: Topology) {

    private val beansById = topology.refs.groupBy { it.id }

    private val beanLinksFrom = topology.links.groupBy { it.from }

    private val beanLinksTo = topology.links.groupBy { it.to }

    fun build(): List<PodRef> {
        val builtPods = mutableListOf<PodRef>()

        for (beanRefs in beansById.values) {
            val ref = beanRefs.first()
            val beanClazz = Class.forName(ref.type).kotlin
            //beanClazz.supertypes.first { it.isSubtypeOf(typeOf<Bean<*, *>>()) }

            val pods: List<PodRef> = when {
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
                            val podProxy = PodProxyRef(podProxyType, listOf(PodKey(link.to, link.toPartition)), beanRef.partition)
                            PodRef(
                                    PodKey(beanRef.id, beanRef.partition),
                                    listOf(beanRef),
                                    listOf(podProxy)

                            )
                        } else {
                            val podProxy = PodProxyRef(podProxyType, links.map { PodKey(it.to, it.toPartition) }, beanRef.partition)
                            if (beanClazz.isSubclassOf(SinglePartitionBean::class) && !beanClazz.isSubclassOf(SinkBean::class)) {
                                PodRef(
                                        PodKey(beanRef.id, beanRef.partition),
                                        listOf(beanRef),
                                        listOf(podProxy),
                                        beanLinksTo.getValue(beanRef.id).count()
                                )
                            } else {
                                throw UnsupportedOperationException("Currently shouldn't be reachable")
                            }
                        }
                    }
                }

                beanClazz.isSubclassOf(SourceBean::class) -> {

                    beanRefs.map { beanRef ->

                        if (beanClazz.isSubclassOf(SinglePartitionBean::class)) {

                            PodRef(
                                    PodKey(beanRef.id, beanRef.partition),
                                    listOf(beanRef),
                                    emptyList(),
                                    beanLinksTo.getValue(beanRef.id).count()
                            )
                        } else {
                            PodRef(
                                    PodKey(beanRef.id, beanRef.partition),
                                    listOf(beanRef),
                                    emptyList()
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

                        PodRef(
                                PodKey(beanRef.id, beanRef.partition),
                                listOf(beanRef),
                                listOf(podProxy1, podProxy2),
                                beanLinksTo.getValue(beanRef.id).count()
                        )
                    }
                }

                else -> throw UnsupportedOperationException("Unsupported class $beanClazz for decorating")
            }

//            println("""
//                BEAN CLASS: $beanClazz
//                #: ${ref.id}
//                POD: $pods
//                POD INPUTS: ${pods.map { it.inputs() }.flatten()}
//                POD INPUT INPUTS: ${pods.map { it.inputs() }.flatten().map { it.inputs() }.flatten()}
//                -----
//                """.trimIndent())
            println("${pods}\n-------")

            builtPods += pods
        }

        return builtPods
    }
}