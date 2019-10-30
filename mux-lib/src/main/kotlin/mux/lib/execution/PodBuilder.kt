package mux.lib.execution

import mux.lib.*
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.typeOf

@ExperimentalStdlibApi
fun Topology.buildPods(): List<AnyPod> = PodBuilder(this).build()


@ExperimentalStdlibApi
class PodBuilder(private val topology: Topology) {

    private val nodeById = topology.refs.map { it.id to it }.toMap()

    private val nodeLinks = topology.links.groupBy { it.from }

    fun build(): List<AnyPod> {
        val pods = mutableListOf<AnyPod>()

        val createdPods = mutableSetOf<PodKey>()
        for (beanRef in topology.refs) {
            val nodeClazz = Class.forName(beanRef.type).kotlin

            check(beanRef.id !in createdPods) { "Topology should already be created without duplication. Found duplicated bean: $beanRef" }
            createdPods += beanRef.id

            val pod = when {
                nodeClazz.isSubclassOf(SingleBean::class) || nodeClazz.isSubclassOf(AlterBean::class) -> {
                    val constructor = nodeClazz.constructors.first {
                        it.parameters.size == 2 &&
                                it.parameters[0].type.isSubtypeOf(typeOf<Bean<*, *>>()) &&
                                it.parameters[1].type.isSubtypeOf(typeOf<BeanParams>())
                    }

                    val podProxyType = constructor.parameters[0].type
                    val links = nodeLinks.getValue(beanRef.id).map { nodeById.getValue(it.to) }
                    require(links.size == 1) { "SingleBean or AlterBean $beanRef should have only one link, but: $links" }

                    val podProxy = PodRegistry.createPodProxy(podProxyType, links[0].id)

                    PodRegistry.createPod(
                            nodeClazz.supertypes.first { it.isSubtypeOf(typeOf<Bean<*, *>>()) },
                            beanRef.id,
                            constructor.call(podProxy, beanRef.params) as Bean<*, *>
                    )
                }

                nodeClazz.isSubclassOf(SourceBean::class) -> {
                    val constructor = nodeClazz.constructors.first {
                        it.parameters.size == 1 &&
                                it.parameters[0].type.isSubtypeOf(typeOf<BeanParams>())
                    }

                    PodRegistry.createPod(
                            nodeClazz.supertypes.first { it.isSubtypeOf(typeOf<Bean<*, *>>()) },
                            beanRef.id,
                            constructor.call(beanRef.params) as Bean<*, *>
                    )
                }

                nodeClazz.isSubclassOf(MultiBean::class) -> {
                    // TODO add support for 2+
                    val constructor = nodeClazz.constructors.first {
                        it.parameters.size == 3 &&
                                it.parameters[0].type.isSubtypeOf(typeOf<Bean<*, *>>()) &&
                                it.parameters[1].type.isSubtypeOf(typeOf<Bean<*, *>>()) &&
                                it.parameters[2].type.isSubtypeOf(typeOf<BeanParams>())
                    }
                    val podProxyType1 = constructor.parameters[0].type
                    val podProxyType2 = constructor.parameters[1].type
                    val links = nodeLinks.getValue(beanRef.id)
                            .sortedBy { it.order }
                            .map { nodeById.getValue(it.to) }
                    require(links.size == 2) { "MergedSampleStream should have only 2 links: $links" }
                    val podProxy1 = PodRegistry.createPodProxy(podProxyType1, links[0].id)
                    val podProxy2 = PodRegistry.createPodProxy(podProxyType2, links[1].id)

                    PodRegistry.createPod(
                            nodeClazz.supertypes.first { it.isSubtypeOf(typeOf<Bean<*, *>>()) },
                            beanRef.id,
                            constructor.call(podProxy1, podProxy2, beanRef.params) as Bean<*, *>
                    )
                }

                else -> throw UnsupportedOperationException("Unsupported class $nodeClazz for decorating")
            }

            println("""
                NODE CLASS: $nodeClazz
                #: ${beanRef.id}
                POD: $pod
                POD INPUTS: ${pod.inputs()}
                POD INPUT INPUTS: ${pod.inputs().map { it.inputs() }}
                -----
                """.trimIndent())

            pods += pod
        }

        return pods
    }
}