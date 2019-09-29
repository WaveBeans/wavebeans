package mux.lib.execution

import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isNotNull
import assertk.assertions.matchesPredicate
import mux.lib.MuxNode
import mux.lib.io.CsvSampleStreamOutputParams
import mux.lib.io.SineGeneratedInputParams
import mux.lib.io.sine
import mux.lib.io.toCsv
import mux.lib.stream.trim
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TopologySpec : Spek({
    describe("2 In 2 Out topology") {

        val ids = mutableMapOf<MuxNode<*, *>, Int>()
        fun <T : MuxNode<*, *>> n(id: Int, node: T): T {
            ids[node] = id
            return node
        }

        fun <T : MuxNode<*, *>> i(id1: Int, id2: Int, node: T): T {
            ids[node.inputs().first()] = id1
            ids[node] = id2
            return node
        }

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

        val topology = outputs.buildTopology(object : IdResolver {
            override fun id(node: MuxNode<*, *>): Int = ids[node] ?: throw IllegalStateException("$node is not found")
        })

        it("should have connection between 6 and 5 nodes") { assertThat(topology.links).contains(MuxNodeLink(6, 5)) }
        it("should have connection between 5 and 2 nodes") { assertThat(topology.links).contains(MuxNodeLink(5, 2)) }
        it("should have connection between 2 and 1 nodes") { assertThat(topology.links).contains(MuxNodeLink(2, 1)) }
        it("should have connection between 8 and 7 nodes") { assertThat(topology.links).contains(MuxNodeLink(8, 7)) }
        it("should have connection between 7 and 4 nodes") { assertThat(topology.links).contains(MuxNodeLink(7, 4)) }
        it("should have connection between 4 and 3 nodes") { assertThat(topology.links).contains(MuxNodeLink(4, 3)) }

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
})