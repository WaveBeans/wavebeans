package io.wavebeans.execution

import assertk.all
import assertk.assertThat
import assertk.assertions.*
import io.wavebeans.lib.AnyBean
import io.wavebeans.lib.io.CsvStreamOutputParams
import io.wavebeans.lib.io.SineGeneratedInputParams
import io.wavebeans.lib.io.sine
import io.wavebeans.lib.io.toCsv
import io.wavebeans.lib.stream.*
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TopologySpec : Spek({
    val ids = mutableMapOf<AnyBean, Long>()

    val idResolver = object : IdResolver {
        override fun id(bean: AnyBean): Long = ids[bean] ?: throw IllegalStateException("$bean is not found")
    }

    beforeGroup {
        ids.clear()
    }


    fun <T : AnyBean> T.n(id: Int): T {
        ids[this] = id.toLong()
        return this
    }

    describe("2 In 2 Out topology") {


        val i1 = 440.sine(0.5).n(1).div(2.0).n(2)
        val i2 = 800.sine(0.0).n(3).div(2.0).n(4)

        val o1 = i1.trim(5000).n(5)
                .toCsv("file:///some1.csv").n(6)
        val o2 = i2.trim(3000).n(7)
                .toCsv("file:///some2.csv").n(8)


        val outputs = listOf(o1, o2)

        val topology = outputs.buildTopology(idResolver)

        it("shouldn't have duplicate beans") {
            assertThat(topology.refs.distinctBy { it.id }).isEqualTo(topology.refs)
        }
        it("should have connection between 6 and 5 nodes") { assertThat(topology.links).contains(BeanLink(6, 5)) }
        it("should have connection between 5 and 2 nodes") { assertThat(topology.links).contains(BeanLink(5, 2)) }
        it("should have connection between 2 and 1 nodes") { assertThat(topology.links).contains(BeanLink(2, 1)) }
        it("should have connection between 8 and 7 nodes") { assertThat(topology.links).contains(BeanLink(8, 7)) }
        it("should have connection between 7 and 4 nodes") { assertThat(topology.links).contains(BeanLink(7, 4)) }
        it("should have connection between 4 and 3 nodes") { assertThat(topology.links).contains(BeanLink(4, 3)) }

        it("should have reference to input 1 node") {
            assertThat(topology.refs.firstOrNull { it.id == 1L })
                    .isNotNull()
                    .all {
                        matchesPredicate { it.type.endsWith("SineGeneratedInput") }
                        matchesPredicate { it.params is SineGeneratedInputParams }
                        matchesPredicate { (it.params as SineGeneratedInputParams).frequency == 440.0 }
                    }
        }

        it("should have reference to input 2 node") {
            assertThat(topology.refs.firstOrNull { it.id == 3L })
                    .isNotNull()
                    .all {
                        matchesPredicate { it.type.endsWith("SineGeneratedInput") }
                        matchesPredicate { it.params is SineGeneratedInputParams }
                        matchesPredicate { (it.params as SineGeneratedInputParams).frequency == 800.0 }
                    }
        }

        it("should have reference to output 1 node") {
            assertThat(topology.refs.firstOrNull { it.id == 6L })
                    .isNotNull()
                    .all {
                        matchesPredicate { it.type.endsWith("CsvStreamOutput") }
                        matchesPredicate { it.params is CsvStreamOutputParams<*> }
                        matchesPredicate { (it.params as CsvStreamOutputParams<*>).uri == "file:///some1.csv" }
                    }
        }

        it("should have reference to output 1 node") {
            assertThat(topology.refs.firstOrNull { it.id == 8L })
                    .isNotNull()
                    .all {
                        matchesPredicate { it.type.endsWith("CsvStreamOutput") }
                        matchesPredicate { it.params is CsvStreamOutputParams<*> }
                        matchesPredicate { (it.params as CsvStreamOutputParams<*>).uri == "file:///some2.csv" }
                    }
        }
    }

    describe("Topology if outputs share parts") {
        describe("Separate outputs") {
            val i = 440.sine().n(1).div(2.0).n(2)
            val p1 = i.changeAmplitude(2.0).n(3)
            val p2 = i.changeAmplitude(3.0).n(4)
            val o1 = p1.trim(3000).n(5).toCsv("file:///some1.csv").n(6)
            val o2 = p2.trim(3000).n(7).toCsv("file:///some2.csv").n(8)

            val topology = listOf(o1, o2).buildTopology(idResolver)
            it("shouldn't have duplicate beans") {
                assertThat(topology.refs.distinctBy { it.id }).isEqualTo(topology.refs)
            }
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

        describe("One output") {
            val i = 440.sine().n(1).div(2.0).n(2)
            val p1 = (i * 2.0).n(3)
            val p2 = (i / 3.0).n(4)
            val o1 = (p1 + p2).n(5)
                    .trim(3000).n(6)
                    .toCsv("file:///some1.csv").n(7)
            val topology = listOf(o1).buildTopology(idResolver)

            it("shouldn't have duplicate beans") {
                assertThat(topology.refs.distinctBy { it.id }).isEqualTo(topology.refs)
            }
        }
    }

    describe("Topology with merging two streams, for restoring state the order of the links to one node matters") {
        val i1 = 440.sine().n(1).div(2.0).n(2)
        val i2 = 880.sine().n(3).div(2.0).n(4)
        val p1 = (i1 + i2).n(5)
        val o = p1.trim(3000).n(7).toCsv("file:///some.csv").n(8)

        val topology = listOf(o).buildTopology(idResolver)

        it("shouldn't have duplicate beans") {
            assertThat(topology.refs.distinctBy { it.id }).isEqualTo(topology.refs)
        }
        it("Link following from 5 to 2 should have order 0") {
            assertThat(topology.links.first { it.from == 5L && it.to == 2L })
                    .prop("order") { it.order }
                    .isEqualTo(0)
        }

        it("Link following from 5 to 4 should have order 1") {
            assertThat(topology.links.first { it.from == 5L && it.to == 4L })
                    .prop("order") { it.order }
                    .isEqualTo(1)
        }

        it("All others links should have order 0") {
            assertThat(topology.links.filterNot { it.from == 5L && (it.to == 4L || it.to == 2L) })
                    .each { nodeLink ->
                        nodeLink.prop("order") { it.order }
                                .isEqualTo(0)

                    }
        }
    }

})