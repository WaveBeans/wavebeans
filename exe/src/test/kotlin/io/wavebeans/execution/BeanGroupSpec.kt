package io.wavebeans.execution

import assertk.Assert
import assertk.all
import assertk.assertThat
import assertk.assertions.*
import io.wavebeans.lib.Bean
import io.wavebeans.execution.TopologySerializer.jsonPretty
import io.wavebeans.lib.AnyBean
import io.wavebeans.lib.io.sine
import io.wavebeans.lib.io.toCsv
import io.wavebeans.lib.stream.div
import io.wavebeans.lib.stream.plus
import io.wavebeans.lib.stream.trim
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import kotlin.reflect.full.isSubclassOf

object BeanGroupSpec : Spek({

    beforeGroup {
        ids.clear()
    }

    describe("Single line topology") {
        val topology = listOf(
                440.sine().i(1, 2)
                        .div(2.0).n(3)
                        .trim(1).n(4)
                        .toCsv("file:///some.csv").n(5)
        )
                .buildTopology(idResolver)
                .groupBeans(groupIdResolver())

        it("should have only one group bean") {
            assertThat(topology.refs).all {
                size().isEqualTo(1)
                group(100).all {
                    groupRefs().ids().isListOf(5, 4, 3, 2, 1)
                    groupLinks().isListOf(5 to 4, 4 to 3, 3 to 2, 2 to 1)
                }
            }
        }
        it("should have no links") { assertThat(topology.links).isEmpty() }
    }

    describe("Topology with merge") {
        val topology = listOf(
                440.sine().i(1, 2)
                        .div(2.0).n(3)
                        .plus(880.sine().i(4, 5)).n(6)
                        .trim(1).n(7)
                        .toCsv("file:///some.csv").n(8)
        )
                .buildTopology(idResolver)
                .groupBeans(groupIdResolver())

        it("should have three group beans") {
            assertThat(topology.refs).all {
                size().isEqualTo(3)
                group(100).all {
                    groupRefs().ids().isListOf(8, 7, 6)
                    groupLinks().isListOf(8 to 7, 7 to 6)
                }
                group(101).all {
                    groupRefs().ids().isListOf(3, 2, 1)
                    groupLinks().isListOf(3 to 2, 2 to 1)
                }
                group(102).all {
                    groupRefs().ids().isListOf(5, 4)
                    groupLinks().isListOf(5 to 4)
                }
            }
        }
        it("should have two links") {
            assertThat(topology.links).isListOf(
                    100 to 101 order 0,
                    100 to 102 order 1
            )
        }

    }

    describe("Topology with multiple outputs. Single line.") {
        val topology = listOf(
                440.sine().i(1, 2).trim(1).n(3).toCsv("file:///some.csv").n(4),
                880.sine().i(5, 6).trim(1).n(7).toCsv("file:///some.csv").n(8)
        )
                .buildTopology(idResolver)
                .groupBeans(groupIdResolver())

        it("should have three group beans") {
            assertThat(topology.refs).all {
                size().isEqualTo(2)
                group(100).all {
                    groupRefs().ids().isListOf(4, 3, 2, 1)
                    groupLinks().isListOf(4 to 3, 3 to 2, 2 to 1)
                }
                group(101).all {
                    groupRefs().ids().isListOf(8, 7, 6, 5)
                    groupLinks().isListOf(8 to 7, 7 to 6, 6 to 5)
                }
            }
        }
        it("should have no links") {
            assertThat(topology.links).isEmpty()
        }

    }

    describe("Topology with multiple outputs. Shared parts") {
        val shared = 440.sine().i(1, 2)
        val topology = listOf(
                shared.trim(1).n(3).toCsv("file:///some.csv").n(4),
                shared.trim(1).n(5).toCsv("file:///some.csv").n(6)
        )
                .buildTopology(idResolver)
                .groupBeans(groupIdResolver())

        it("should have three group beans") {
            assertThat(topology.refs).all {
                size().isEqualTo(3)
                group(100).all {
                    groupRefs().ids().isListOf(4, 3)
                    groupLinks().isListOf(4 to 3)
                }
                group(101).all {
                    groupRefs().ids().isListOf(2, 1)
                    groupLinks().isListOf(2 to 1)
                }
                group(102).all {
                    groupRefs().ids().isListOf(6, 5)
                    groupLinks().isListOf(6 to 5)
                }
            }
        }
        it("should have two links") {
            assertThat(topology.links).isListOf(
                    100 to 101,
                    102 to 101
            )
        }

    }

    describe("Topology with split-merge in the middle") {
        //-----------------------------------------------------------------------
        // (i1)<-(inf2)<---        v---(trim6)<--(o7)
        //                 |-(plus5)
        // (i3)<-(inf4)<---        ^---(trim8)<--(o9)
        //-----------------------------------------------------------------------
        val i1 = 440.sine().i(1, 2)
        val i2 = 880.sine().i(3, 4)
        val plus = (i1 + i2).n(5)
        val o1 = plus.trim(1).n(6).toCsv("file:///some1.csv").n(7)
        val o2 = plus.trim(1).n(8).toCsv("file:///some2.csv").n(9)

        //-----------------------------------------------------------------------
        // [101]<---        v---[100]
        //          |-(plus5)
        // [102]<---        ^---[103]
        //-----------------------------------------------------------------------
        val topology = listOf(o1, o2)
                .buildTopology(idResolver)
                .groupBeans(groupIdResolver())
                .also { println(TopologySerializer.serialize(it, jsonPretty)) }


        it("should have five group beans") {
            assertThat(topology.refs).all {
                size().isEqualTo(5)
                group(100).all {
                    groupRefs().ids().isListOf(7, 6)
                    groupLinks().isListOf(7 to 6)
                }
                group(101).all {
                    groupRefs().ids().isListOf(2, 1)
                    groupLinks().isListOf(2 to 1)
                }
                bean(5).isNotNull()
                group(103).all {
                    groupRefs().ids().isListOf(9, 8)
                    groupLinks().isListOf(9 to 8)
                }
                group(102).all {
                    groupRefs().ids().isListOf(4, 3)
                    groupLinks().isListOf(4 to 3)
                }
            }
        }
        it("should have four links") {
            assertThat(topology.links).isListOf(
                    100 to 5,
                    5 to 101 order 0,
                    5 to 102 order 1,
                    103 to 5
            )
        }
    }

    describe("Single line topology. Two partitions") {
        val topology = listOf(
                440.sine().i(1, 2)                     // (i1.0) <-(inf2.0) <-(inf2.1)
                        .div(2.0).n(3)               //             ^-(div3.0)  ^-(div3.1)
                        .trim(1).n(4)                //                  ^-----------^---(trim4.0)
                        .toCsv("file:///some.csv").n(5) //                                     ^-(o5.0)
        )
                .buildTopology(idResolver)
                .partition(2)
                .groupBeans(groupIdResolver())

        it("should have only one group bean") {
            assertThat(topology.refs).all {
                size().isEqualTo(4)
                group(100).all {
                    groupRefs().ids().isListOf(5, 4)
                    groupLinks().isListOf(5.0 to 4.0)
                }
                group(101.0).all {
                    groupRefs().ids().isListOf(3, 2)
                    groupLinks().isListOf(3.0 to 2.0)
                }
                group(101.1).all {
                    groupRefs().ids().isListOf(3, 2)
                    groupLinks().isListOf(3.1 to 2.1)
                }
                bean(1).isNotNull()
            }
        }
        it("should have four links") {
            assertThat(topology.links).isListOf(
                    100.0 to 101.0,
                    100.0 to 101.1,
                    101.0 to 1.0,
                    101.1 to 1.0
            )
        }
    }

    describe("Topology with merge. Two partitions") {
        //-----------------------------------------------------------------------
        // (i1.0) <-(inf2.0) <-(inf2.1)                       <| [101.0] <|
        //             ^-------------------(div3.0)           <|          | [101.1]
        //                       ^----------------(div3.1)               <|
        // (i4.0) <-(inf5.0) <-(inf5.1)
        //            ^---------^-(plus6.0)--^------^                  <|
        //                           ^-(trim7.0)                       <| [100.0]
        //                                 ^-(o8.0)                    <|
        //-----------------------------------------------------------------------
        val topology = listOf(
                440.sine().i(1, 2)
                        .div(2.0).n(3)
                        .plus(
                                880.sine().i(4, 5)
                        ).n(6)
                        .trim(1).n(7)
                        .toCsv("file:///some.csv").n(8)
        )
                //-----------------------------------------------------------------------
                // (i1.0)                       <-[101.0]   <-[101.1]
                // (i4.0) <-(inf5.0)  <-(inf5.1)
                //             ^----------^-[100.0]-^-----------^
                //-----------------------------------------------------------------------
                .buildTopology(idResolver)
                .partition(2)
                .groupBeans(groupIdResolver())

        it("should have seven group beans") {
            assertThat(topology.refs).all {
                size().isEqualTo(7)
                group(100).all {
                    groupRefs().ids().isListOf(8, 7, 6)
                    groupLinks().isListOf(8 to 7, 7 to 6)
                }
                bean(5.0).isNotNull()
                bean(5.1).isNotNull()
                bean(4.0).isNotNull()
                group(101.0).all {
                    groupRefs().ids().isListOf(3, 2)
                    groupLinks().isListOf(3.0 to 2.0)
                }
                group(101.1).all {
                    groupRefs().ids().isListOf(3, 2)
                    groupLinks().isListOf(3.1 to 2.1)
                }
                bean(1.0).isNotNull()
            }
        }
        it("should have eight links") {
            assertThat(topology.links.sortedWith(compareBy({ it.from }, { it.to })).also { println(it) })
                    .isListOf(
                            5.0 to 4.0,
                            5.1 to 4.0,
                            100.0 to 5.0 order 1,
                            100.0 to 5.1 order 1,
                            100.0 to 101.0 order 0,
                            100.0 to 101.1 order 0,
                            101.0 to 1.0,
                            101.1 to 1.0
                    )
        }

    }


})

private val ids = mutableMapOf<AnyBean, Int>()

private val idResolver = object : IdResolver {
    override fun id(bean: AnyBean): Int = ids[bean] ?: throw IllegalStateException("$bean is not found")
}

private fun <T : AnyBean> T.n(id: Int): T {
    ids[this] = id
    return this
}

private fun <T : AnyBean> T.i(id1: Int, id2: Int): T {
    ids[this.inputs().first()] = id1
    ids[this] = id2
    return this
}

private fun groupIdResolver() = object : GroupIdResolver {
    var groupIdSeq = 100
    override fun id(): Int = groupIdSeq++
}

private fun Assert<List<BeanRef>>.bean(id: Int): Assert<BeanRef?> =
        this.prop("[$id]") { it.singleOrNull { it.id == id } }

private fun Assert<List<BeanRef>>.bean(id: Double): Assert<BeanRef?> =
        this.prop("[$id]") { it.singleOrNull { it.id == id.toInt() && it.partition == (id * 10.0).toInt() % 10 } }

private fun Assert<List<BeanRef>>.group(id: Int): Assert<BeanRef> =
        this.bean(id)
                .isNotNull()
                .also {
                    it.prop("type") { Class.forName(it.type).kotlin }
                            .matchesPredicate { it.isSubclassOf(BeanGroup::class) }
                }

private fun Assert<List<BeanRef>>.group(id: Double): Assert<BeanRef> =
        this.bean(id)
                .isNotNull()
                .also {
                    it.prop("type") { Class.forName(it.type).kotlin }
                            .matchesPredicate { it.isSubclassOf(BeanGroup::class) }
                }

private fun Assert<BeanRef>.groupRefs(): Assert<List<BeanRef>> =
        this.prop("params") { it.params }
                .isInstanceOf(BeanGroupParams::class)
                .prop("groupedBeansRefs") { it.beanRefs }

private fun Assert<List<BeanRef>>.ids(): Assert<List<Int>> =
        this.transform { list -> list.map { it.id } }

private fun Assert<BeanRef>.groupLinks(): Assert<List<BeanLink>> =
        this.prop("params") { it.params }
                .isInstanceOf(BeanGroupParams::class)
                .prop("internalLinks") { it.links }
