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
    describe("") {
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

        val endpoints = mutableMapOf<Int, PodEndpoint<*, *>>()
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

                    val channelType = constructor.parameters[0].type
                    val links = nodeLinks.getValue(nodeRef.id).map { nodeById.getValue(it.to) }
                    require(links.size == 1) { "SingleBean or AlterBean $nodeRef should have only one link, but: $links" }

                    val channel = PodRegistry.createPod(channelType, links[0].id)

                    val endpoint = PodRegistry.createPodEndpoint(
                            nodeClazz.supertypes.first { it.isSubtypeOf(typeOf<Bean<*, *>>()) },
                            constructor.call(channel, nodeRef.params) as Bean<*, *>
                    )
                    println(endpoint)

                    endpoints[nodeRef.id] = endpoint
                }

                nodeClazz.isSubclassOf(SourceBean::class) -> {
                    val constructor = nodeClazz.constructors.first {
                        it.parameters.size == 1 &&
                                it.parameters[0].type.isSubtypeOf(typeOf<BeanParams>())
                    }

                    val endpoint = PodRegistry.createPodEndpoint(
                            nodeClazz.supertypes.first { it.isSubtypeOf(typeOf<Bean<*, *>>()) },
                            constructor.call(nodeRef.params) as Bean<*, *>
                    )
                    println(endpoint)

                    endpoints[nodeRef.id] = endpoint
                }

                nodeClazz.isSubclassOf(MultiBean::class) -> {
                    // TODO add support for 2+
                    val constructor = nodeClazz.constructors.first {
                        it.parameters.size == 3 &&
                                it.parameters[0].type.isSubtypeOf(typeOf<Bean<*, *>>()) &&
                                it.parameters[1].type.isSubtypeOf(typeOf<Bean<*, *>>()) &&
                                it.parameters[2].type.isSubtypeOf(typeOf<BeanParams>())
                    }
                    val channel1Type = constructor.parameters[0].type
                    val channel2Type = constructor.parameters[1].type
                    val links = nodeLinks.getValue(nodeRef.id)
                            .sortedBy { it.order }
                            .map { nodeById.getValue(it.from) }
                    require(links.size == 2) { "MergedSampleStream should have only 2 links: $links" }
                    val channel1 = PodRegistry.createPod(channel1Type, links[0].id)
                    val channel2 = PodRegistry.createPod(channel2Type, links[1].id)

                    val endpoint = PodRegistry.createPodEndpoint(
                            nodeClazz.supertypes.first { it.isSubtypeOf(typeOf<Bean<*, *>>()) },
                            constructor.call(channel1, channel2, nodeRef.params) as Bean<*, *>
                    )
                    println(endpoint)

                    endpoints[nodeRef.id] = endpoint

                }
            }
            println("------")

        }

        Bush().use { bush ->
            endpoints.forEach { (t, u) ->
                bush.addPodEndpoint(t, u)
            }
            bush.start()


            PodDiscovery.endpoints()
                    .filter { it.endpoint is StreamOutput<*, *> }
                    .map { it.endpoint as StreamOutput<*, *> }
                    .map {
                        it.writer(44100.0f)
                    }
                    .forEach {
                        it.write(1000)
                    }
        }

    }
})