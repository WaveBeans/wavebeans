package mux.lib.execution

import assertk.Assert
import assertk.all
import assertk.assertThat
import assertk.assertions.*
import assertk.assertions.support.fail
import mux.lib.Bean
import mux.lib.io.sine
import mux.lib.io.toCsv
import mux.lib.stream.div
import mux.lib.stream.plus
import mux.lib.stream.trim
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import kotlin.reflect.full.isSubclassOf

object BeanGroupSpec : Spek({
    val ids = mutableMapOf<Bean<*, *>, Int>()

    val idResolver = object : IdResolver {
        override fun id(bean: Bean<*, *>): Int = ids[bean] ?: throw IllegalStateException("$bean is not found")
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

    describe("Single line topology") {
        val topology = listOf(
                440.sine().i(1, 2)
                        .div(2.0).n(3)
                        .trim(1).n(4)
                        .toCsv("file:///some.csv").n(5)
        ).buildTopology(idResolver)

        val newTopology = topology.groupBeans(groupIdResolver())

        it("should have only one group bean") {
            assertThat(newTopology.refs).all {
                size().isEqualTo(1)
                group(100).all {
                    groupRefs().ids().isListOf(5, 4, 3, 2, 1)
                    groupLinks().isListOf(5 to 4, 4 to 3, 3 to 2, 2 to 1)
                }
            }
        }
        it("should have no links") { assertThat(newTopology.links).isEmpty() }
    }

    describe("Topology with merge") {
        val topology = listOf(
                440.sine().i(1, 2)
                        .div(2.0).n(3)
                        .plus(880.sine().i(4, 5)).n(6)
                        .trim(1).n(7)
                        .toCsv("file:///some.csv").n(8)
        ).buildTopology(idResolver)

        val newTopology = topology.groupBeans(groupIdResolver())
        it("should have three group beans") {
            assertThat(newTopology.refs).all {
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
            assertThat(newTopology.links).isListOf(
                    100 to 101 order 0,
                    100 to 102 order 1
            )
        }

    }
})

private fun groupIdResolver() = object : GroupIdResolver {
    var groupIdSeq = 100
    override fun id(): Int = groupIdSeq++
}

private fun <T> Assert<List<T>>.isListOf(vararg expected: Any?) = given { actual ->
    if (actual == expected.toList()) return
    fail(expected, actual)
}

private infix fun Int.to(to: Int) = BeanLink(this, to)
private infix fun BeanLink.order(order: Int) = this.copy(order = order)

private fun Assert<List<BeanRef>>.group(id: Int): Assert<BeanRef> =
        this.prop("group[$id]") { it.firstOrNull { it.id == id } }
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
