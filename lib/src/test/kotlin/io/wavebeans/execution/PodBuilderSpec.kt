package io.wavebeans.execution

import assertk.Assert
import assertk.all
import assertk.assertThat
import assertk.assertions.*
import io.wavebeans.lib.Bean
import io.wavebeans.execution.TopologySerializer.jsonPretty
import io.wavebeans.lib.eachIndexed
import io.wavebeans.lib.io.sine
import io.wavebeans.lib.io.toCsv
import io.wavebeans.lib.stream.*
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

@ExperimentalStdlibApi
class PodBuilderSpec : Spek({
    val ids = mutableMapOf<Bean<*, *>, Int>()

    val idResolver = object : IdResolver {
        override fun id(node: Bean<*, *>): Int = ids[node] ?: throw IllegalStateException("$node is not found")
    }

    beforeGroup {
        ids.clear()
    }


    fun <T : Bean<*, *>> T.n(id: Int): T {
        ids[this] = id
        return this
    }

    fun <T : Bean<*, *>> T.i(id1: Int, id2: Int): T {
        ids[this.inputs().first()] = id1
        ids[this] = id2
        return this
    }


    describe("Building pods on simple topology") {
        val i1 = 440.sine(0.5).i(1, 2)
        val i2 = 800.sine(0.0).i(3, 4)

        val o1 = i1.trim(5000).n(5)
                .toCsv("file:///some1.csv").n(6)
        val o2 =
                i2.trim(3000).n(7)
                        .toCsv("file:///some2.csv").n(8)

        val pods = listOf(o1, o2)
                .buildTopology(idResolver)
                .buildPods()

        it("should have 8 pods") { assertThat(pods).size().isEqualTo(8) }
        it("should have 8 unique ids") {
            assertThat(pods.map { it.key.id }.distinct().sorted()).isEqualTo((1..8).toList())
        }
    }

    describe("Building pods on topology with shared parts") {
        val i = 440.sine().i(1, 2)
        val p1 = (i * 2.0).n(3)
        val p2 = (i * 3.0).n(4)
        val o1 = p1.trim(3000).n(5).toCsv("file:///some1.csv").n(6)
        val o2 = p2.trim(3000).n(7).toCsv("file:///some2.csv").n(8)

        val pods = listOf(o1, o2)
                .buildTopology(idResolver)
                .buildPods()

        it("should have 8 pods") { assertThat(pods).size().isEqualTo(8) }
        it("should have 8 unique ids") {
            assertThat(pods.map { it.key.id }.distinct().sorted()).isEqualTo((1..8).toList())
        }
    }

    describe("Building pods on partitioned topology. Linear topology") {
        val o1 =
                440.sine().i(1, 2) // 1 + partitionCount
                        .div(3.0).n(3) // partitionCount
                        .trim(3000).n(4) // 1
                        .toCsv("file:///some1.csv").n(5) // 1

        val pods = listOf(o1)
                .buildTopology(idResolver)
                .partition(2)
                .buildPods()

        it("should have 8 pods") { assertThat(pods).size().isEqualTo(7) }
        it("should have 5 unique ids") {
            assertThat(pods.map { it.key.id }.distinct().sorted()).isEqualTo((1..5).toList())
        }
    }

    describe("Building pods on partitioned topology. Merging topology") {
        val o1 =
                440.sine().i(1, 2) // 1 + partitionCount
                        .div(3.0).n(3) // partitionCount
                        .plus(
                                880.sine().i(4, 5) // 1 + partitionCount
                        ).n(6) // 1
                        .trim(3000).n(7) // 1
                        .toCsv("file:///some1.csv").n(8) // 1

        val pods = listOf(o1)
                .buildTopology(idResolver)
                .partition(2)
                .buildPods()

        it("should have 12 pods") { assertThat(pods).size().isEqualTo(11) }
        it("should have 8 unique ids") {
            assertThat(pods.map { it.key.id }.distinct().sorted()).isEqualTo((1..8).toList())
        }
        it("should have correct links -- proxies and partitions") {
            assertThat(pods).all {
                podWithKey("output", 8).all {
                    eachIndexed(1) { p, podPartitionIdx ->
                        p.splitToPartitions().isNull()
                        p.podProxies().eachIndexed(1) { a, i ->
                            when (i) {
                                0 -> a.streamingPodProxyKey().isEqualTo(PodKey(7, 0))
                            }
                            a.forPartition().isEqualTo(podPartitionIdx)
                        }
                    }
                    eachIndexed { pod, i -> pod.partition().isEqualTo(i) }

                }
                podWithKey("trim", 7).all {
                    eachIndexed(1) { p, podPartitionIdx ->
                        p.splitToPartitions().isNull()
                        p.podProxies().eachIndexed(1) { a, podProxyIndex ->
                            when (Pair(podProxyIndex, podPartitionIdx)) {
                                Pair(0, 0) -> a.streamingPodProxyKey().isEqualTo(PodKey(6, 0))
                            }
                            a.forPartition().isEqualTo(podPartitionIdx)
                        }
                    }
                    eachIndexed { pod, i -> pod.partition().isEqualTo(i) }
                }
                podWithKey("plus", 6).all {
                    eachIndexed(1) { it, podPartitionIdx ->
                        it.splitToPartitions().isNull()
                        it.podProxies().eachIndexed(2) { a, i ->
                            when (i) {
                                0 -> a.mergingPodProxyKeys().isEqualTo(setOf(PodKey(3, 0), PodKey(3, 1)))
                                1 -> a.mergingPodProxyKeys().isEqualTo(setOf(PodKey(5, 0), PodKey(5, 1)))
                            }
                            a.forPartition().isEqualTo(podPartitionIdx)
                        }
                    }
                    eachIndexed { pod, i -> pod.partition().isEqualTo(i) }
                }
                podWithKey("inf for sine 880", 5).all {
                    eachIndexed(2) { p, podPartitionIdx ->
                        p.splitToPartitions().isNull()
                        p.podProxies().eachIndexed(1) { a, podProxyIdx ->
                            when (Pair(podProxyIdx, podPartitionIdx)) {
                                Pair(0, 0) -> a.streamingPodProxyKey().isEqualTo(PodKey(4, 0))
                                Pair(0, 1) -> a.streamingPodProxyKey().isEqualTo(PodKey(4, 0))
                            }
                            a.forPartition().isEqualTo(podPartitionIdx)
                        }
                    }
                    eachIndexed { pod, i -> pod.partition().isEqualTo(i) }
                }
                podWithKey("sine 880", 4).all {
                    size().isEqualTo(1)
                    each {
                        it.splitToPartitions().isEqualTo(2)
                        it.podProxies().isEmpty()
                    }
                    eachIndexed { pod, i -> pod.partition().isEqualTo(i) }
                }
                podWithKey("div", 3).all {
                    eachIndexed(2) { p, podPartitionIdx ->
                        p.splitToPartitions().isNull()
                        p.podProxies().eachIndexed(1) { a, podProxyIndex ->
                            when (Pair(podProxyIndex, podPartitionIdx)) {
                                Pair(0, 0) -> a.streamingPodProxyKey().isEqualTo(PodKey(2, 0))
                                Pair(0, 1) -> a.streamingPodProxyKey().isEqualTo(PodKey(2, 1))
                            }
                            a.forPartition().isEqualTo(podPartitionIdx)
                        }
                    }
                    eachIndexed { pod, i -> pod.partition().isEqualTo(i) }
                }
                podWithKey("inf for sine 440", 2).all {
                    eachIndexed(2) { p, podPartitionIdx ->
                        p.splitToPartitions().isNull()
                        p.podProxies().eachIndexed(1) { a, podProxyIdx ->
                            when (Pair(podProxyIdx, podPartitionIdx)) {
                                Pair(0, 0) -> a.streamingPodProxyKey().isEqualTo(PodKey(1, 0))
                                Pair(0, 1) -> a.streamingPodProxyKey().isEqualTo(PodKey(1, 0))
                            }
                            a.forPartition().isEqualTo(podPartitionIdx)
                        }
                    }
                    eachIndexed { pod, i -> pod.partition().isEqualTo(i) }
                }
                podWithKey("sine 440", 1).all {
                    each {
                        it.splitToPartitions().isEqualTo(2)
                        it.podProxies().isEmpty()
                    }
                    size().isEqualTo(1)
                    eachIndexed { pod, i -> pod.partition().isEqualTo(i) }
                }
            }
        }
    }

    describe("Building pods on partitioned topology. Two outputs") {
        val i = 440.sine().i(1, 2)
        val o1 = i.trim(1).n(3).toCsv("file:///some.csv").n(4)
        val o2 = i.trim(1).n(5).toCsv("file:///some.csv").n(6)
        val pods = listOf(o1, o2)
                .buildTopology(idResolver)
                .partition(2)
                .buildPods()

        it("should have 7 pods") { assertThat(pods).size().isEqualTo(7) }
        it("should have 6 unique ids") {
            assertThat(pods.map { it.key.id }.distinct().sorted()).isEqualTo((1..6).toList())
        }
        it("should have pods of specific type") {
            assertThat(pods).all {
                podWithKey("output1", 4).all {
                    eachIndexed(1) { p, _ ->
                        p.splitToPartitions().isNull()
                        p.podProxies().eachIndexed(1) { proxy, _ ->
                            proxy.streamingPodProxyKey().isEqualTo(PodKey(3, 0))
                            proxy.forPartition().isEqualTo(0)
                        }
                    }
                }
                podWithKey("trim1", 3).all {
                    eachIndexed(1) { p, _ ->
                        p.splitToPartitions().isNull()
                        p.podProxies().eachIndexed(1) { proxy, _ ->
                            proxy.mergingPodProxyKeys().isEqualTo(setOf(PodKey(2, 0), PodKey(2, 1)))
                            proxy.forPartition().isEqualTo(0)
                        }
                    }
                }
                podWithKey("output2", 6).all {
                    eachIndexed(1) { p, _ ->
                        p.splitToPartitions().isNull()
                        p.podProxies().eachIndexed(1) { proxy, _ ->
                            proxy.streamingPodProxyKey().isEqualTo(PodKey(5, 0))
                            proxy.forPartition().isEqualTo(0)
                        }
                    }
                }
                podWithKey("trim2", 5).all {
                    eachIndexed(1) { p, _ ->
                        p.splitToPartitions().isNull()
                        p.podProxies().eachIndexed(1) { proxy, _ ->
                            proxy.mergingPodProxyKeys().isEqualTo(setOf(PodKey(2, 0), PodKey(2, 1)))
                            proxy.forPartition().isEqualTo(0)
                        }
                    }
                }
                podWithKey("inf", 2).all {
                    eachIndexed(2) { p, i ->
                        p.splitToPartitions().isNull()
                        p.podProxies().eachIndexed(1) { proxy, _ ->
                            proxy.streamingPodProxyKey().isEqualTo(PodKey(1, 0))
                            proxy.forPartition().isEqualTo(i)
                        }
                    }
                }
                podWithKey("sine", 1).all {
                    eachIndexed(1) { p, i ->
                        p.splitToPartitions().isEqualTo(2)
                        p.podProxies().isEmpty()
                    }
                }
            }
        }
    }

    describe("Building pods on grouped topology") {

        describe("Single line topology that fits into 1 group.") {
            val pods = listOf(
                    440.sine().i(1, 2)
                            .trim(1).n(3)
                            .toCsv("file:///some.csv").n(4)
            )
                    .buildTopology(idResolver)
                    .groupBeans(groupIdResolver())
                    .buildPods()

            it("should have one pod") { assertThat(pods).size().isEqualTo(1) }
            it("should have unique id") {
                assertThat(pods.map { it.key.id }.distinct().sorted()).isEqualTo((100..100).toList())
            }
            it("should have pods of specific type") {
                assertThat(pods).all {
                    podWithKey("group bean", 100).all {
                        eachIndexed(1) { podRef, _ ->
                            podRef.podProxies().isEmpty()
                            podRef.partition().isEqualTo(0)
                        }
                    }
                }
            }
        }

        describe("Partitioned single line topology") {
            val pods = listOf(
                    440.sine().i(1, 2)                       // (1.0) <| (2.0) (2.1)
                            .trim(1).n(3)                    // <| [100.0]
                            .toCsv("file:///some.csv").n(4)  // <|
            )
                    .buildTopology(idResolver)
                    .partition(2)
                    .groupBeans(groupIdResolver())
                    .buildPods()

            it("should have 4 pods") { assertThat(pods).size().isEqualTo(4) }
            it("should have unique id") {
                assertThat(pods.map { it.key.id }.distinct().sorted()).isEqualTo((listOf(1, 2, 100)))
            }
            it("should have pods of specific type") {
                assertThat(pods).all {
                    podWithKey("group bean", 100).all {
                        eachIndexed(1) { podRef, _ ->
                            podRef.podProxies().eachIndexed(1) { proxy, _ ->
                                proxy.mergingPodProxyKeys().isEqualTo(setOf(PodKey(2, 0), PodKey(2, 1)))
                            }
                            podRef.partition().isEqualTo(0)
                        }
                    }
                    podWithKey("inf", 2).all {
                        eachIndexed(2) { podRef, i ->
                            podRef.podProxies().eachIndexed(1) { proxy, _ ->
                                proxy.streamingPodProxyKey().isEqualTo(PodKey(1, 0))
                            }
                            podRef.partition().isEqualTo(i)
                        }
                    }
                    podWithKey("sine", 1).all {
                        eachIndexed(1) { podRef, _ ->
                            podRef.podProxies().isEmpty()
                            podRef.partition().isEqualTo(0)
                        }
                    }
                }
            }
        }

        describe("Merging topology") {
            val pods = listOf(
                    440.sine().i(1, 2)                         // <| [101]
                            .plus(
                                    440.sine().i(3, 4)         //   <|[102]
                            ).n(5)                             // <|
                            .trim(1).n(6)                      // <| [100]
                            .toCsv("file:///some.csv").n(7)    // <|
            )
                    .buildTopology(idResolver)
                    .groupBeans(groupIdResolver())
                    .buildPods()

            it("should have 4 pods") { assertThat(pods).size().isEqualTo(3) }
            it("should have unique ids") {
                assertThat(pods.map { it.key.id }.distinct().sorted()).isEqualTo((100..102).toList())
            }
            it("should have pods of specific type") {
                assertThat(pods).all {
                    podWithKey("group [csv->trim->plus]", 100).all {
                        eachIndexed(1) { podRef, _ ->
                            podRef.podProxies().eachIndexed(2) { proxy, j ->
                                when (j) {
                                    0 -> proxy.streamingPodProxyKey().isEqualTo(PodKey(101, 0))
                                    1 -> proxy.streamingPodProxyKey().isEqualTo(PodKey(102, 0))
                                }
                            }
                            podRef.partition().isEqualTo(0)
                        }
                    }
                    podWithKey("group [inf->sine1]", 101).all {
                        eachIndexed(1) { podRef, _ ->
                            podRef.podProxies().isEmpty()
                            podRef.partition().isEqualTo(0)
                        }
                    }
                    podWithKey("group [inf->sine2]", 102).all {
                        eachIndexed(1) { podRef, _ ->
                            podRef.podProxies().isEmpty()
                            podRef.partition().isEqualTo(0)
                        }
                    }
                }
            }
        }

        describe("Merging topology with shared parts. Partitioned") {
            val i = 440.sine().i(1, 2)                          // (1) <| [101.0] [101.1]
                    .changeAmplitude(0.5).n(3)                  //
                    .plus(                                      //
                            440.sine().i(4, 5)                  // (4) <|[102.0] [102.1]
                                    .changeAmplitude(1.3).n(6)  //     <|
                    ).n(7)                                      // (7)

            val pods = listOf(
                    i.trim(1).n(8)                              // <| [100]
                            .toCsv("file:///some.csv").n(9),    // <|
                    i.trim(1).n(10)                             //   <|[103]
                            .toCsv("file:///some.csv").n(11)    //   <|
            )
                    .buildTopology(idResolver)
                    .partition(2)
                    .groupBeans(groupIdResolver())
                    .also { println(TopologySerializer.serialize(it, jsonPretty)) }
                    .buildPods()

            it("should have 9 pods") { assertThat(pods).size().isEqualTo(9) }
            it("should have unique ids") {
                assertThat(pods.map { it.key.id }.distinct().sorted()).isEqualTo(listOf(1, 4, 7) + (100..103).toList())
            }
            it("should have pods of specific type") {
                assertThat(pods).all {
                    podWithKey("group [csv1->trim1]", 100).all {
                        eachIndexed(1) { podRef, _ ->
                            podRef.podProxies().eachIndexed(1) { proxy, _ ->
                                proxy.streamingPodProxyKey().isEqualTo(PodKey(7, 0))
                            }
                            podRef.partition().isEqualTo(0)
                        }
                    }
                    podWithKey("group [csv2->trim2]", 103).all {
                        eachIndexed(1) { podRef, _ ->
                            podRef.podProxies().eachIndexed(1) { proxy, _ ->
                                proxy.streamingPodProxyKey().isEqualTo(PodKey(7, 0))
                            }
                            podRef.partition().isEqualTo(0)
                        }
                    }
                    podWithKey("group [mult1->inf1]", 101).all {
                        eachIndexed(2) { podRef, i ->
                            podRef.podProxies().eachIndexed(1) { proxy, _ ->
                                proxy.streamingPodProxyKey().isEqualTo(PodKey(1, 0))
                            }
                            podRef.partition().isEqualTo(i)
                        }
                    }
                    podWithKey("group [mult2->inf2]", 102).all {
                        eachIndexed(2) { podRef, i ->
                            podRef.podProxies().eachIndexed(1) { proxy, _ ->
                                proxy.streamingPodProxyKey().isEqualTo(PodKey(4, 0))
                            }
                            podRef.partition().isEqualTo(i)
                        }
                    }
                    podWithKey("plus", 7).all {
                        eachIndexed(1) { podRef, _ ->
                            podRef.podProxies().eachIndexed(2) { proxy, i ->
                                when (i) {
                                    0 -> proxy.mergingPodProxyKeys().isEqualTo(setOf(PodKey(101, 0), PodKey(101, 1)))
                                    1 -> proxy.mergingPodProxyKeys().isEqualTo(setOf(PodKey(102, 0), PodKey(102, 1)))
                                }
                            }
                            podRef.partition().isEqualTo(0)
                        }
                    }
                    podWithKey("sine1", 1).all {
                        eachIndexed(1) { podRef, _ ->
                            podRef.podProxies().isEmpty()
                            podRef.partition().isEqualTo(0)
                        }
                    }
                    podWithKey("sine2", 4).all {
                        eachIndexed(1) { podRef, _ ->
                            podRef.podProxies().isEmpty()
                            podRef.partition().isEqualTo(0)
                        }
                    }
                }
            }
        }
    }
})

private fun groupIdResolver() = object : GroupIdResolver {
    var groupIdSeq = 100
    override fun id(): Int = groupIdSeq++
}


private fun Assert<PodRef>.partition() = this.prop("partition") { it.key.partition }
private fun Assert<PodRef>.splitToPartitions() = this.prop("splitToPartitions") { it.splitToPartitions }

private fun Assert<List<PodRef>>.podWithKey(name: String, id: Int) =
        prop(name) { ps -> ps.filter { it.key.id == id }.sortedBy { it.key.partition } }

private fun Assert<PodRef>.podProxies() =
        prop("pod proxies") { it.podProxies }

private fun Assert<PodProxyRef>.mergingPodProxyKeys() =
        this.prop("podKeys") { it.pointedTo.toSet() }

private fun Assert<PodProxyRef>.streamingPodProxyKey() =
        this.prop("podKeys") { it.pointedTo.first() }

private fun Assert<PodProxyRef>.forPartition() =
        this.prop("forPartition") { it.partition }
