package mux.lib.execution

import assertk.Assert
import assertk.all
import assertk.assertThat
import assertk.assertions.*
import mux.lib.Bean
import mux.lib.eachIndexed
import mux.lib.io.sine
import mux.lib.io.toCsv
import mux.lib.stream.*
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
            assertThat(pods.map { it.podKey.id }.distinct().sorted()).isEqualTo((1..8).toList())
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
            assertThat(pods.map { it.podKey.id }.distinct().sorted()).isEqualTo((1..8).toList())
        }
    }

    describe("Building pods on partitioned topology. Linear topology") {
        val o1 =
                440.sine().i(1, 2) // 1 + partitionCount
                        .div(3.0).n(3) // partitionCount
                        .trim(3000).n(4) // partitionCount
                        .toCsv("file:///some1.csv").n(5) // 1

        val pods = listOf(o1)
                .buildTopology(idResolver)
                .partition(2)
                .buildPods()

        it("should have 8 pods") { assertThat(pods).size().isEqualTo(8) }
        it("should have 5 unique ids") {
            assertThat(pods.map { it.podKey.id }.distinct().sorted()).isEqualTo((1..5).toList())
        }
    }
    describe("Building pods on partitioned topology. Merging topology") {
        val o1 =
                440.sine().i(1, 2) // 1 + partitionCount
                        .div(3.0).n(3) // partitionCount
                        .plus(
                                880.sine().i(4, 5) // 1 + partitionCount
                        ).n(6) // 1
                        .trim(3000).n(7) // partitionCount
                        .toCsv("file:///some1.csv").n(8) // 1

        val pods = listOf(o1)
                .buildTopology(idResolver)
                .partition(2)
                .buildPods()

        it("should have 12 pods") { assertThat(pods).size().isEqualTo(12) }
        it("should have 8 unique ids") {
            assertThat(pods.map { it.podKey.id }.distinct().sorted()).isEqualTo((1..8).toList())
        }
        it("should have pods of specific type") {
            assertThat(pods).all {
                podWithKey("output", 8).all {
                    size().isEqualTo(1)
                    each {
                        it.isInstanceOf(SampleStreamOutputPod::class)
                        it.podProxies().eachIndexed(1) { a, i ->
                            when (i) {
                                0 -> a.mergingPodProxyKeys().isEqualTo(setOf(PodKey(7, 0), PodKey(7, 1)))
                            }
                        }
                    }
                    eachIndexed { pod, i -> pod.partition().isEqualTo(i) }

                }
                podWithKey("trim", 7).all {
                    eachIndexed(2) { p, podIndex ->
                        p.isInstanceOf(StreamingPod::class)
                        p.podProxies().eachIndexed(1) { a, podProxyIndex ->
                            when (Pair(podProxyIndex, podIndex)) {
                                Pair(0, 0) -> a.streamingPodProxyKey().isEqualTo(PodKey(6, 0))
                                Pair(0, 1) -> a.streamingPodProxyKey().isEqualTo(PodKey(6, 0))
                            }
                        }
                    }
                    eachIndexed { pod, i -> pod.partition().isEqualTo(i) }
                }
                podWithKey("plus", 6).all {
                    each {
                        it.isInstanceOf(SplittingPod::class)
                                .prop("partitionCount") { pod -> pod.partitionCount }.isEqualTo(2)
                        it.podProxies().eachIndexed(2) { a, i ->
                            when (i) {
                                0 -> a.mergingPodProxyKeys().isEqualTo(setOf(PodKey(3, 0), PodKey(3, 1)))
                                1 -> a.mergingPodProxyKeys().isEqualTo(setOf(PodKey(5, 0), PodKey(5, 1)))
                            }
                        }
                    }
                    size().isEqualTo(1)
                    eachIndexed { pod, i -> pod.partition().isEqualTo(i) }
                }
                podWithKey("inf for sine 880", 5).all {
                    eachIndexed(2) { p, podIndex ->
                        p.isInstanceOf(StreamingPod::class)
                        p.podProxies().eachIndexed(1) { a, podProxyIndex ->
                            when (Pair(podProxyIndex, podIndex)) {
                                Pair(0, 0) -> a.streamingPodProxyKey().isEqualTo(PodKey(4, 0))
                                Pair(0, 1) -> a.streamingPodProxyKey().isEqualTo(PodKey(4, 0))
                            }
                        }
                    }
                    size().isEqualTo(2)
                    eachIndexed { pod, i -> pod.partition().isEqualTo(i) }
                }
                podWithKey("sine 880", 4).all {
                    size().isEqualTo(1)
                    each {
                        it.isInstanceOf(SplittingPod::class)
                                .prop("partitionCount") { pod -> pod.partitionCount }.isEqualTo(2)
                        it.podProxies().isEmpty()
                    }
                    eachIndexed { pod, i -> pod.partition().isEqualTo(i) }
                }
                podWithKey("div", 3).all {
                    eachIndexed(2) { p, podIndex ->
                        p.isInstanceOf(StreamingPod::class)
                        p.podProxies().eachIndexed(1) { a, podProxyIndex ->
                            when (Pair(podProxyIndex, podIndex)) {
                                Pair(0, 0) -> a.streamingPodProxyKey().isEqualTo(PodKey(2, 0))
                                Pair(0, 1) -> a.streamingPodProxyKey().isEqualTo(PodKey(2, 1))
                            }
                        }
                    }
                    eachIndexed { pod, i -> pod.partition().isEqualTo(i) }
                }
                podWithKey("inf for sine 440", 2).all {
                    eachIndexed(2) { p, podIndex ->
                        p.isInstanceOf(StreamingPod::class)
                        p.podProxies().eachIndexed(1) { a, podProxyIndex ->
                            when (Pair(podProxyIndex, podIndex)) {
                                Pair(0, 0) -> a.streamingPodProxyKey().isEqualTo(PodKey(1, 0))
                                Pair(0, 1) -> a.streamingPodProxyKey().isEqualTo(PodKey(1, 0))
                            }
                        }
                    }
                    eachIndexed { pod, i -> pod.partition().isEqualTo(i) }
                }
                podWithKey("sine 440", 1).all {
                    each {
                        it.isInstanceOf(SplittingPod::class)
                                .prop("partitionCount") { pod -> pod.partitionCount }.isEqualTo(2)
                        it.podProxies().isEmpty()
                    }
                    size().isEqualTo(1)
                    eachIndexed { pod, i -> pod.partition().isEqualTo(i) }
                }
            }
        }
    }
})

private fun Assert<AnyPod>.partition() = this.prop("partition") { it.podKey.partition }

private fun Assert<List<AnyPod>>.podWithKey(name: String, id: Int) =
        prop(name) { ps -> ps.filter { it.podKey.id == id }.sortedBy { it.podKey.partition } }

private fun Assert<AnyPod>.podProxies() =
        prop("pod proxies") { ps ->
            ps.inputs()
                    .map { it.inputs() }.flatten()
                    .map { it as PodProxy<*, *> }
                    .toList()
        }

private fun Assert<AnyPodProxy>.mergingPodProxyKeys() =
        this.isInstanceOf(MergingPodProxy::class)
                .prop("podKeys") { it.readsFrom.toSet() }

private fun Assert<AnyPodProxy>.streamingPodProxyKey() =
        this.isInstanceOf(StreamingPodProxy::class)
                .prop("podKeys") { it.pointedTo }
