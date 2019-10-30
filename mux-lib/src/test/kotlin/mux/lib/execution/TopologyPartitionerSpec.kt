package mux.lib.execution

import assertk.all
import assertk.assertThat
import assertk.assertions.*
import mux.lib.Bean
import mux.lib.io.*
import mux.lib.stream.div
import mux.lib.stream.plus
import mux.lib.stream.times
import mux.lib.stream.trim
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TopologyPartitionerSpec : Spek({

    val maxPartitions = 10
    val partitionedIdsOffset = 100
    val ids = mutableMapOf<Bean<*, *>, Int>()

    val partitioningIdResolver = object : PartitioningIdResolver {
        override fun id(beanRef: BeanRef, partition: Int): Int {
            return partitionedIdsOffset + beanRef.id * maxPartitions + partition
        }
    }

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

    fun Topology.beansForId(id: Int) = this.refs.filter { (it.id - partitionedIdsOffset) / maxPartitions == id }

    fun Topology.links(from: Int, to: Int): List<BeanLink> {
        val fromIds = this.beansForId(from).map { it.id }
        val toIds = this.beansForId(to).map { it.id }
        return this.links
                .filter { it.from in fromIds && it.to in toIds }
    }

    fun Topology.partitionLinks(from: Int, pFrom: Int, to: Int, pTo: Int): List<BeanLink> {
        val from = this.beansForId(from).filter { it.partition == pFrom }.map { it.id }
        val to = this.beansForId(to).filter { it.partition == pTo }.map { it.id }
        return this.links.filter { it.from in from && it.to in to }
    }

    describe("Simple one-line topology") {
        val i1 = 440.sine(0.5).i(1, 2)
        val o1 = i1.trim(5000).n(3).toCsv("file:///some1.csv").n(4)

        val topology = listOf(o1).buildTopology(idResolver)

        it("should remain the same if partitionsCount=1") {
            assertThat(topology.partition(1, partitioningIdResolver))
                    .all {
                        prop("Bean[id=1]") { it.beansForId(1) }.size().isEqualTo(1)
                        prop("Bean[id=2]") { it.beansForId(2) }.size().isEqualTo(1)
                        prop("Bean[id=3]") { it.beansForId(3) }.size().isEqualTo(1)
                        prop("Bean[id=4]") { it.beansForId(4) }.size().isEqualTo(1)
                        prop("Link[2->1]") { it.links(2, 1) }.size().isEqualTo(1)
                        prop("Link[3->2]") { it.links(3, 2) }.size().isEqualTo(1)
                        prop("Link[4->3]") { it.links(4, 3) }.size().isEqualTo(1)
                    }
        }

        it("should duplicate some of beans if partitionsCount=2") {
            assertThat(topology.partition(2, partitioningIdResolver))
                    .all {
                        prop("Bean[id=1]") { it.beansForId(1) }.size().isEqualTo(1)
                        prop("Bean[id=2]") { it.beansForId(2) }.size().isEqualTo(2)
                        prop("Bean[id=3]") { it.beansForId(3) }.size().isEqualTo(2)
                        prop("Bean[id=4]") { it.beansForId(4) }.size().isEqualTo(1)
                        prop("Link[2->1]") { it.links(2, 1) }.size().isEqualTo(2)
                        prop("Link[3->2]") { it.links(3, 2) }.size().isEqualTo(2)
                        prop("Link[4->3]") { it.links(4, 3) }.size().isEqualTo(2)
                        prop("Link[2.0->1.0]") { it.partitionLinks(2, 0, 1, 0) }.size().isEqualTo(1)
                        prop("Link[2.1->1.0]") { it.partitionLinks(2, 1, 1, 0) }.size().isEqualTo(1)
                        prop("Link[3.0->2.0]") { it.partitionLinks(3, 0, 2, 0) }.size().isEqualTo(1)
                        prop("Link[3.1->2.1]") { it.partitionLinks(3, 1, 2, 1) }.size().isEqualTo(1)
                        prop("Link[4.0->3.0]") { it.partitionLinks(4, 0, 3, 0) }.size().isEqualTo(1)
                        prop("Link[4.0->3.1]") { it.partitionLinks(4, 0, 3, 1) }.size().isEqualTo(1)
                    }
        }

        it("should duplicate some of beans if partitionsCount=5") {
            assertThat(topology.partition(5, partitioningIdResolver))
                    .all {
                        prop("Bean[id=1]") { it.beansForId(1) }.size().isEqualTo(1)
                        prop("Bean[id=2]") { it.beansForId(2) }.size().isEqualTo(5)
                        prop("Bean[id=3]") { it.beansForId(3) }.size().isEqualTo(5)
                        prop("Bean[id=4]") { it.beansForId(4) }.size().isEqualTo(1)
                        prop("Link[2->1]") { it.links(2, 1) }.size().isEqualTo(5)
                        prop("Link[3->2]") { it.links(3, 2) }.size().isEqualTo(5)
                        prop("Link[4->3]") { it.links(4, 3) }.size().isEqualTo(5)
                        prop("Link[2.0->1.0]") { it.partitionLinks(2, 0, 1, 0) }.size().isEqualTo(1)
                        prop("Link[2.1->1.0]") { it.partitionLinks(2, 1, 1, 0) }.size().isEqualTo(1)
                        prop("Link[2.2->1.0]") { it.partitionLinks(2, 2, 1, 0) }.size().isEqualTo(1)
                        prop("Link[2.3->1.0]") { it.partitionLinks(2, 3, 1, 0) }.size().isEqualTo(1)
                        prop("Link[2.4->1.0]") { it.partitionLinks(2, 4, 1, 0) }.size().isEqualTo(1)
                        prop("Link[3.0->2.0]") { it.partitionLinks(3, 0, 2, 0) }.size().isEqualTo(1)
                        prop("Link[3.1->2.1]") { it.partitionLinks(3, 1, 2, 1) }.size().isEqualTo(1)
                        prop("Link[3.2->2.2]") { it.partitionLinks(3, 2, 2, 2) }.size().isEqualTo(1)
                        prop("Link[3.3->2.3]") { it.partitionLinks(3, 3, 2, 3) }.size().isEqualTo(1)
                        prop("Link[3.4->2.4]") { it.partitionLinks(3, 4, 2, 4) }.size().isEqualTo(1)
                        prop("Link[4.0->3.0]") { it.partitionLinks(4, 0, 3, 0) }.size().isEqualTo(1)
                        prop("Link[4.0->3.1]") { it.partitionLinks(4, 0, 3, 1) }.size().isEqualTo(1)
                        prop("Link[4.0->3.2]") { it.partitionLinks(4, 0, 3, 2) }.size().isEqualTo(1)
                        prop("Link[4.0->3.3]") { it.partitionLinks(4, 0, 3, 3) }.size().isEqualTo(1)
                        prop("Link[4.0->3.4]") { it.partitionLinks(4, 0, 3, 4) }.size().isEqualTo(1)
                    }
        }

    }

    describe("Merging topology") {
        val i1 = 440.sine(0.5).i(1, 2)
        val i2 = 880.sine(0.5).i(3, 4)
        val o1 = (i1 + i2).n(5).trim(5000).n(6).toCsv("file:///some1.csv").n(7)

        val topology = listOf(o1).buildTopology(idResolver)

        it("should remain the same if partitionsCount=1") {
            assertThat(topology.partition(1, partitioningIdResolver))
                    .all {
                        prop("Bean[id=1]") { it.beansForId(1) }.size().isEqualTo(1)
                        prop("Bean[id=2]") { it.beansForId(2) }.size().isEqualTo(1)
                        prop("Bean[id=3]") { it.beansForId(3) }.size().isEqualTo(1)
                        prop("Bean[id=4]") { it.beansForId(4) }.size().isEqualTo(1)
                        prop("Bean[id=5]") { it.beansForId(5) }.size().isEqualTo(1)
                        prop("Bean[id=6]") { it.beansForId(6) }.size().isEqualTo(1)
                        prop("Bean[id=7]") { it.beansForId(7) }.size().isEqualTo(1)
                        prop("Link[2->1]") { it.links(2, 1) }.size().isEqualTo(1)
                        prop("Link[4->3]") { it.links(4, 3) }.size().isEqualTo(1)
                        prop("Link[5->4]") { it.links(5, 4) }.size().isEqualTo(1)
                        prop("Link[5->2]") { it.links(5, 2) }.size().isEqualTo(1)
                        prop("Link[6->5]") { it.links(6, 5) }.size().isEqualTo(1)
                        prop("Link[7->6]") { it.links(7, 6) }.size().isEqualTo(1)
                    }
        }

        it("should duplicate some of beans if partitionsCount=2") {
            assertThat(topology.partition(2, partitioningIdResolver))
                    .all {
                        prop("Bean[id=1]") { it.beansForId(1) }.size().isEqualTo(1)
                        prop("Bean[id=2]") { it.beansForId(2) }.size().isEqualTo(2)
                        prop("Bean[id=3]") { it.beansForId(3) }.size().isEqualTo(1)
                        prop("Bean[id=4]") { it.beansForId(4) }.size().isEqualTo(2)
                        prop("Bean[id=5]") { it.beansForId(5) }.size().isEqualTo(1)
                        prop("Bean[id=6]") { it.beansForId(6) }.size().isEqualTo(2)
                        prop("Bean[id=7]") { it.beansForId(7) }.size().isEqualTo(1)
                        prop("Link[2->1]") { it.links(2, 1) }.size().isEqualTo(2)
                        prop("Link[4->3]") { it.links(4, 3) }.size().isEqualTo(2)
                        prop("Link[5.0->4.0]") { it.partitionLinks(5, 0, 4, 0) }.size().isEqualTo(1)
                        prop("Link[5.0->4.1]") { it.partitionLinks(5, 0, 4, 1) }.size().isEqualTo(1)
                        prop("Link[5.0->2.0]") { it.partitionLinks(5, 0, 2, 0) }.size().isEqualTo(1)
                        prop("Link[5.0->2.1]") { it.partitionLinks(5, 0, 2, 1) }.size().isEqualTo(1)
                        prop("Link[6.0->5.0]") { it.partitionLinks(6, 0, 5, 0) }.size().isEqualTo(1)
                        prop("Link[6.1->5.0]") { it.partitionLinks(6, 1, 5, 0) }.size().isEqualTo(1)
                        prop("Link[7.0->6.0]") { it.partitionLinks(7, 0, 6, 0) }.size().isEqualTo(1)
                        prop("Link[7.0->6.1]") { it.partitionLinks(7, 0, 6, 1) }.size().isEqualTo(1)
                    }
        }
    }

    describe("Topology with shared parts") {
        val i = 440.sine().i(1, 2)
        val p1 = (i * 2.0).n(3)
        val p2 = (i / 3.0).n(4)
        val o1 = (p1 + p2).n(5)
                .trim(3000).n(6)
                .toCsv("file:///some1.csv").n(7)

        val topology = listOf(o1).buildTopology(idResolver)
        it("should remain the same if partitionsCount=1") {
            assertThat(topology.partition(1, partitioningIdResolver))
                    .all {
                        prop("Bean[id=1]") { it.beansForId(1) }.size().isEqualTo(1)
                        prop("Bean[id=2]") { it.beansForId(2) }.size().isEqualTo(1)
                        prop("Bean[id=3]") { it.beansForId(3) }.size().isEqualTo(1)
                        prop("Bean[id=4]") { it.beansForId(4) }.size().isEqualTo(1)
                        prop("Bean[id=5]") { it.beansForId(5) }.size().isEqualTo(1)
                        prop("Bean[id=6]") { it.beansForId(6) }.size().isEqualTo(1)
                        prop("Bean[id=7]") { it.beansForId(7) }.size().isEqualTo(1)
                        prop("Link[2->1]") { it.links(2, 1) }.size().isEqualTo(1)
                        prop("Link[3->2]") { it.links(3, 2) }.size().isEqualTo(1)
                        prop("Link[4->2]") { it.links(4, 2) }.size().isEqualTo(1)
                        prop("Link[5->3]") { it.links(5, 3) }.size().isEqualTo(1)
                        prop("Link[5->4]") { it.links(5, 4) }.size().isEqualTo(1)
                        prop("Link[6->5]") { it.links(6, 5) }.size().isEqualTo(1)
                        prop("Link[7->6]") { it.links(7, 6) }.size().isEqualTo(1)
                    }
        }
        it("should duplicate some of beans if partitionsCount=2") {
            assertThat(topology.partition(2, partitioningIdResolver))
                    .all {
                        prop("Bean[id=1]") { it.beansForId(1) }.size().isEqualTo(1)
                        prop("Bean[id=2]") { it.beansForId(2) }.size().isEqualTo(2)
                        prop("Bean[id=3]") { it.beansForId(3) }.size().isEqualTo(2)
                        prop("Bean[id=4]") { it.beansForId(4) }.size().isEqualTo(2)
                        prop("Bean[id=5]") { it.beansForId(5) }.size().isEqualTo(1)
                        prop("Bean[id=6]") { it.beansForId(6) }.size().isEqualTo(2)
                        prop("Bean[id=7]") { it.beansForId(7) }.size().isEqualTo(1)
                        prop("Link[2->1]") { it.links(2, 1) }.size().isEqualTo(2)
                        prop("Link[3.0->2.0]") { it.partitionLinks(3, 0, 2, 0) }.size().isEqualTo(1)
                        prop("Link[3.1->2.1]") { it.partitionLinks(3, 1, 2, 1) }.size().isEqualTo(1)
                        prop("Link[4.0->2.0]") { it.partitionLinks(4, 0, 2, 0) }.size().isEqualTo(1)
                        prop("Link[4.1->2.1]") { it.partitionLinks(4, 1, 2, 1) }.size().isEqualTo(1)
                        prop("Link[5->3]") { it.links(5, 3) }.size().isEqualTo(2)
                        prop("Link[5->4]") { it.links(5, 4) }.size().isEqualTo(2)
                        prop("Link[6->5]") { it.links(6, 5) }.size().isEqualTo(2)
                        prop("Link[7->6]") { it.links(7, 6) }.size().isEqualTo(2)
                    }
        }
    }
})