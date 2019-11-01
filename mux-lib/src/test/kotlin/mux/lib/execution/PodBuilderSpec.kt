package mux.lib.execution

import assertk.assertThat
import assertk.assertions.*
import mux.lib.Bean
import mux.lib.execution.TopologySerializer.jsonPretty
import mux.lib.io.CsvSampleStreamOutput
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
            assertThat(pods.map { it.podKey }.distinct().sorted()).isEqualTo((1..8).toList())
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
            assertThat(pods.map { it.podKey }.distinct().sorted()).isEqualTo((1..8).toList())
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
            assertThat(pods.map { it.podKey }.distinct().sorted()).isEqualTo((1..5).toList())
        }
    }
    describe("Building pods on partitioned topology. Merging topology") {
        val o1 =
                440.sine().i(1, 2) // 1 + partitionCount
                        .div(3.0).n(3) // partitionCount
                        .plus (
                                880.sine().i(4,5) // 1 + partitionCount
                        ).n(6) // 1
                        .trim(3000).n(7) // partitionCount
                        .toCsv("file:///some1.csv").n(8) // 1

        val pods = listOf(o1)
                .buildTopology(idResolver)
                .partition(2)
                .buildPods()

        it("should have 12 pods") { assertThat(pods).size().isEqualTo(12) }
        it("should have 8 unique ids") {
            assertThat(pods.map { it.podKey }.distinct().sorted()).isEqualTo((1..8).toList())
        }
    }
})