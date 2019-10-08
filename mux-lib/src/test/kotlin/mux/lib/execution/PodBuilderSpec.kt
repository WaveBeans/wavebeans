package mux.lib.execution

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.size
import mux.lib.Bean
import mux.lib.io.sine
import mux.lib.io.toCsv
import mux.lib.stream.changeAmplitude
import mux.lib.stream.trim
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

@ExperimentalStdlibApi
class PodBuilderSpec : Spek({
    val ids = mutableMapOf<Bean<*, *>, Int>()
    fun <T : Bean<*, *>> n(id: Int, node: T): T {
        ids[node] = id
        return node
    }

    fun <T : Bean<*, *>> i(id1: Int, id2: Int, node: T): T {
        ids[node.inputs().first()] = id1
        ids[node] = id2
        return node
    }

    val idResolver = object : IdResolver {
        override fun id(node: Bean<*, *>): Int = ids[node] ?: throw IllegalStateException("$node is not found")
    }

    beforeGroup {
        ids.clear()
    }

    describe("Building pods on simple topology") {
        val i1 = i(1, 2, 440.sine(0.5))
        val i2 = i(3, 4, 800.sine(0.0))

        val o1 =
                n(6,
                        n(5,
                                i1.trim(5000)
                        )
                                .toCsv("file:///some1.csv")
                )
        val o2 =
                n(8,
                        n(7,
                                i2.trim(3000)
                        )
                                .toCsv("file:///some2.csv")
                )


        val pods = listOf(o1, o2)
                .buildTopology(idResolver)
                .buildPods()

        it("should have 8 pods") { assertThat(pods).size().isEqualTo(8) }
        it("should have 8 unique ids") {
            assertThat(pods.map { it.podKey }.distinct().sorted()).isEqualTo((1..8).toList())
        }
    }

    describe("Building pods on topology with shared parts") {
        val i = i(1, 2, 440.sine())
        val p1 = n(3, i.changeAmplitude(2.0))
        val p2 = n(4, i.changeAmplitude(3.0))
        val o1 = n(6, n(5, p1.trim(3000)).toCsv("file:///some1.csv"))
        val o2 = n(8, n(7, p2.trim(3000)).toCsv("file:///some2.csv"))

        val pods = listOf(o1, o2)
                .buildTopology(idResolver)
                .buildPods()

        it("should have 8 pods") { assertThat(pods).size().isEqualTo(8) }
        it("should have 8 unique ids") {
            assertThat(pods.map { it.podKey }.distinct().sorted()).isEqualTo((1..8).toList())
        }
    }

})