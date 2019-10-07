package mux.lib

import mux.lib.execution.*
import mux.lib.io.StreamOutput
import mux.lib.io.sine
import mux.lib.io.toCsv
import mux.lib.stream.changeAmplitude
import mux.lib.stream.plus
import mux.lib.stream.trim
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.typeOf

@ExperimentalStdlibApi
object BeanRuntimeSpec : Spek({
    describe("bean") {
        val outputs = ArrayList<StreamOutput<*, *>>()


        val i1 = 440.sine(0.5)
        val i2 = 800.sine(0.0)

        val p1 = i1.changeAmplitude(1.7)
        val p2 = i2.changeAmplitude(1.8)
                .rangeProjection(0, 1000)

        val o1 = p1
                .trim(5000)
                .toCsv("file:///users/asubb/tmp/o1.csv")
        val o2 = (p1 + p2)
                .trim(3000)
                .toCsv("file:///users/asubb/tmp/o2.csv")


        outputs += o1
        outputs += o2

        val topology = outputs.buildTopology()
        println("Topology: $topology")

        val nodeById = topology.refs.map { it.id to it }.toMap()
        val nodeLinks = topology.links.groupBy { it.from }

        fun nodeClazz(ref: BeanRef): KClass<out Any> = Class.forName(ref.type).kotlin

        val pods = mutableMapOf<Int, Pod<*, *>>()
        topology.refs.forEach { nodeRef ->
            val nodeClazz = nodeClazz(nodeRef)
            println(nodeClazz)
            when {

                nodeClazz.isSubclassOf(SingleBean::class) || nodeClazz.isSubclassOf(AlterBean::class) -> {
                    val constructor = nodeClazz.constructors.first {
                        it.parameters.size == 2 &&
                                it.parameters[0].type.isSubtypeOf(typeOf<Bean<*, *>>()) &&
                                it.parameters[1].type.isSubtypeOf(typeOf<BeanParams>())
                    }

                    val podProxyType = constructor.parameters[0].type
                    val links = nodeLinks.getValue(nodeRef.id).map { nodeById.getValue(it.to) }
                    require(links.size == 1) { "SingleBean or AlterBean $nodeRef should have only one link, but: $links" }

                    val podProxy = PodRegistry.createPodProxy(podProxyType, links[0].id)

                    val pod = PodRegistry.createPod(
                            nodeClazz.supertypes.first { it.isSubtypeOf(typeOf<Bean<*, *>>()) },
                            nodeRef.id,
                            constructor.call(podProxy, nodeRef.params) as Bean<*, *>
                    )
                    println("#: " + nodeRef.id)
                    println("POD: " + pod)
                    println("POD INPUTS: " + pod.inputs())
                    println("POD INPUT INPUTS: " + pod.inputs().map { it.inputs() })

                    pods[nodeRef.id] = pod
                }

                nodeClazz.isSubclassOf(SourceBean::class) -> {
                    val constructor = nodeClazz.constructors.first {
                        it.parameters.size == 1 &&
                                it.parameters[0].type.isSubtypeOf(typeOf<BeanParams>())
                    }

                    val pod = PodRegistry.createPod(
                            nodeClazz.supertypes.first { it.isSubtypeOf(typeOf<Bean<*, *>>()) },
                            nodeRef.id,
                            constructor.call(nodeRef.params) as Bean<*, *>
                    )
                    println("#: " + nodeRef.id)
                    println("POD: " + pod)
                    println("POD INPUTS: " + pod.inputs())
                    println("POD INPUT INPUTS: " + pod.inputs().map { it.inputs() })

                    pods[nodeRef.id] = pod
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
                    val links = nodeLinks.getValue(nodeRef.id)
                            .sortedBy { it.order }
                            .map { nodeById.getValue(it.to) }
                    require(links.size == 2) { "MergedSampleStream should have only 2 links: $links" }
                    val podProxy1 = PodRegistry.createPodProxy(podProxyType1, links[0].id)
                    val podProxy2 = PodRegistry.createPodProxy(podProxyType2, links[1].id)

                    val pod = PodRegistry.createPod(
                            nodeClazz.supertypes.first { it.isSubtypeOf(typeOf<Bean<*, *>>()) },
                            nodeRef.id,
                            constructor.call(podProxy1, podProxy2, nodeRef.params) as Bean<*, *>
                    )
                    println("#: " + nodeRef.id)
                    println("POD: " + pod)
                    println("POD INPUTS: " + pod.inputs())
                    println("POD INPUT INPUTS: " + pod.inputs().map { it.inputs() })

                    pods[nodeRef.id] = pod

                }
            }
            println("------")
        }

        Bush().use { bush ->
            pods.forEach { (t, u) ->
                bush.addPod(t, u)
            }

            PodDiscovery.pods().asSequence()
                    .filter { it.pod is StreamOutput<*, *> }
                    .map { it.pod as StreamOutput<*, *> }
                    .map { Pair(it, it.writer(44100.0f)) }
                    .map { it.second.write(100); it }
                    .forEach { it.first.close(); it.second.close() }
        }

    }
})