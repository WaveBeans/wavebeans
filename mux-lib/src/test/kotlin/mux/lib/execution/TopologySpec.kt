package mux.lib.execution

import assertk.all
import assertk.assertThat
import assertk.assertions.*
import mux.lib.Bean
import mux.lib.io.CsvSampleStreamOutputParams
import mux.lib.io.SineGeneratedInputParams
import mux.lib.io.sine
import mux.lib.io.toCsv
import mux.lib.stream.changeAmplitude
import mux.lib.stream.plus
import mux.lib.stream.trim
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TopologySpec : Spek({
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

    describe("2 In 2 Out topology") {


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


        val outputs = listOf(o1, o2)

        val topology = outputs.buildTopology(idResolver)

        it("should have connection between 6 and 5 nodes") { assertThat(topology.links).contains(BeanLink(6, 5)) }
        it("should have connection between 5 and 2 nodes") { assertThat(topology.links).contains(BeanLink(5, 2)) }
        it("should have connection between 2 and 1 nodes") { assertThat(topology.links).contains(BeanLink(2, 1)) }
        it("should have connection between 8 and 7 nodes") { assertThat(topology.links).contains(BeanLink(8, 7)) }
        it("should have connection between 7 and 4 nodes") { assertThat(topology.links).contains(BeanLink(7, 4)) }
        it("should have connection between 4 and 3 nodes") { assertThat(topology.links).contains(BeanLink(4, 3)) }

        it("should have reference to input 1 node") {
            assertThat(topology.refs.firstOrNull { it.id == 1 })
                    .isNotNull()
                    .all {
                        matchesPredicate { it.type.endsWith("SineGeneratedInput") }
                        matchesPredicate { it.params is SineGeneratedInputParams }
                        matchesPredicate { (it.params as SineGeneratedInputParams).frequency == 440.0 }
                    }
        }

        it("should have reference to input 2 node") {
            assertThat(topology.refs.firstOrNull { it.id == 3 })
                    .isNotNull()
                    .all {
                        matchesPredicate { it.type.endsWith("SineGeneratedInput") }
                        matchesPredicate { it.params is SineGeneratedInputParams }
                        matchesPredicate { (it.params as SineGeneratedInputParams).frequency == 800.0 }
                    }
        }

        it("should have reference to output 1 node") {
            assertThat(topology.refs.firstOrNull { it.id == 6 })
                    .isNotNull()
                    .all {
                        matchesPredicate { it.type.endsWith("CsvSampleStreamOutput") }
                        matchesPredicate { it.params is CsvSampleStreamOutputParams }
                        matchesPredicate { (it.params as CsvSampleStreamOutputParams).uri == "file:///some1.csv" }
                    }
        }

        it("should have reference to output 1 node") {
            assertThat(topology.refs.firstOrNull { it.id == 8 })
                    .isNotNull()
                    .all {
                        matchesPredicate { it.type.endsWith("CsvSampleStreamOutput") }
                        matchesPredicate { it.params is CsvSampleStreamOutputParams }
                        matchesPredicate { (it.params as CsvSampleStreamOutputParams).uri == "file:///some2.csv" }
                    }
        }
    }

    describe("Topology if outputs share parts") {
        val i = i(1, 2, 440.sine())
        val p1 = n(3, i.changeAmplitude(2.0))
        val p2 = n(4, i.changeAmplitude(3.0))
        val o1 = n(6, n(5, p1.trim(3000)).toCsv("file:///some1.csv"))
        val o2 = n(8, n(7, p2.trim(3000)).toCsv("file:///some2.csv"))

        val topology = listOf(o1, o2).buildTopology(idResolver)
        it("shouldn't have duplicate links") {
            val expectedLinks = arrayOf(
                    BeanLink(2, 1),
                    BeanLink(3, 2),
                    BeanLink(4, 2),
                    BeanLink(5, 3),
                    BeanLink(6, 5),
                    BeanLink(7, 4),
                    BeanLink(8, 7)
            )
            assertThat(topology.links).all {
                size().isEqualTo(expectedLinks.size)
                each {
                    it.isIn(*expectedLinks)
                }
            }
        }
    }

    describe("Topology with merging two streams, for restoring state the order of the links to one node matters") {
        val i1 = i(1, 2, 440.sine())
        val i2 = i(3, 4, 880.sine())
        val p1 = n(5, i1 + i2)
        val o = n(8, n(7, p1.trim(3000)).toCsv("file:///some.csv"))

        val topology = listOf(o).buildTopology(idResolver)

        it("Link following from 5 to 2 should have order 0") {
            assertThat(topology.links.first { it.from == 5 && it.to == 2 })
                    .prop("order") { it.order }
                    .isEqualTo(0)
        }

        it("Link following from 5 to 4 should have order 1") {
            assertThat(topology.links.first { it.from == 5 && it.to == 4 })
                    .prop("order") { it.order }
                    .isEqualTo(1)
        }

        it("All others links should have order 0") {
            assertThat(topology.links.filterNot { it.from == 5 && (it.to == 4 || it.to == 2) })
                    .each { nodeLink ->
                        nodeLink.prop("order") { it.order }
                                .isEqualTo(0)

                    }
        }
    }

})