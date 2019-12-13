package io.wavebeans.execution

import kotlinx.serialization.Serializable
import io.wavebeans.lib.BeanParams
import io.wavebeans.lib.SinkBean
import kotlin.reflect.full.isSubclassOf

interface BeanGroup

@Serializable
data class BeanGroupParams(
        val beanRefs: List<BeanRef>,
        val links: List<BeanLink>
) : BeanParams()

interface GroupIdResolver {
    fun id(): Int
}

internal class DefaultGroupIdResolver(topology: Topology) : GroupIdResolver {

    private var idSeq = topology.refs.map { it.id }.max() ?: 0

    override fun id(): Int = ++idSeq

}

fun Topology.groupBeans(idResolver: GroupIdResolver = DefaultGroupIdResolver(this)): Topology {
    val beanLinksTo = this.links.groupBy { it.to }
    val beanLinksFrom = this.links.groupBy { it.from }
    val beans = this.refs.groupBy { it.id }

    val strokes = mutableListOf<List<BeanRef>>()

    fun buildStrokes(bean: BeanRef, currentStroke: List<BeanRef>, currentPartition: Int) {
        val linksToBean = beanLinksTo[bean.id]?.filter { it.toPartition == currentPartition } ?: emptyList()
        val linksFromBean = beanLinksFrom[bean.id]?.filter { it.fromPartition == currentPartition } ?: emptyList()
        when {
            linksFromBean.size > 1 && linksToBean.size > 1 -> {
                //split-merge case. The bean should be left intact in its own stroke
                if (currentStroke.isNotEmpty()) strokes += currentStroke
                strokes += listOf(bean)
                val nextBeans = linksFromBean.map { link -> beans.getValue(link.to).single { link.toPartition == it.partition } }
                for (b in nextBeans) {
                    buildStrokes(b, emptyList(), b.partition)
                }
            }
            linksFromBean.size > 1 -> { // merge, stroke finished on this element
                strokes += currentStroke + bean
                val nextBeans = linksFromBean.map { link -> beans.getValue(link.to).single { link.toPartition == it.partition } }
                for (b in nextBeans) {
                    buildStrokes(b, emptyList(), b.partition)
                }
            }
            linksToBean.size > 1 -> { // split operation, stroke has finished on previous element
                strokes += currentStroke
                val nextBeans = linksFromBean.map { link -> beans.getValue(link.to).single { link.toPartition == it.partition } }
                for (b in nextBeans) {
                    buildStrokes(b, listOf(bean), b.partition)
                }
                if (nextBeans.isEmpty()) // in case of input we need to add it as a stroke manually
                    strokes += listOf(bean)
            }
            linksFromBean.size == 1 -> {
                // stroke continues within current partition
                val nextBean = beans.getValue(linksFromBean.single().to).let { beanRefs ->
                    beanRefs.singleOrNull { it.partition == currentPartition }
                            ?: beanRefs.first() // that might be 1-to-1 connection or split
                }
                buildStrokes(
                        nextBean,
                        currentStroke + bean,
                        nextBean.partition
                )

            }
            linksFromBean.isEmpty() && currentStroke.isNotEmpty() -> {
                // finish stroke as there is nowhere to go -- reached the head
                strokes += currentStroke + bean
            }
            linksToBean.size == 1 && currentStroke.isEmpty() -> {
                // single input bean case
                strokes += listOf(bean)
            }
            else -> throw UnsupportedOperationException("Combination is not supported linksFromBean=$linksFromBean, " +
                    "linksToBean=$linksToBean, currentStroke=$currentStroke")
        }
    }

    this.refs
            .filter { Class.forName(it.type).kotlin.isSubclassOf(SinkBean::class) }
            .forEach {
                buildStrokes(it, emptyList(), it.partition)
            }

    // create new bean structure
    val replacedBeans = mutableMapOf<Int, BeanRef>() // locate the group which absorbed the bean by its id
    val groups = mutableMapOf<Set<Int>, Int>() // locate group by its internal ids
    val beanRefs = strokes
            .filter { it.isNotEmpty() }
            .distinct()
            .map { stroke ->
                if (stroke.size > 1) {
                    val strokePartition = stroke.first().partition
                    val groupInternalIds = stroke.map { it.id }.toSet()
                    val id = groups[groupInternalIds] ?: idResolver.id()
                    val beanGroupRef = BeanRef.create(
                            id = id,
                            type = BeanGroup::class,
                            parameters = BeanGroupParams(
                                    stroke,
                                    this.links.filter {
                                        it.from in groupInternalIds && it.to in groupInternalIds
                                                && it.fromPartition == strokePartition && it.toPartition == strokePartition
                                    }
                            ),
                            partition = strokePartition
                    )
                    groups[groupInternalIds] = id
                    stroke.forEach { replacedBeans[it.id] = beanGroupRef }
                    beanGroupRef
                } else {
                    stroke.first()
                }
            }

    val beanLinks = this.links
            .map {
                val newTo = replacedBeans[it.to]?.id ?: it.to
                val newFrom = replacedBeans[it.from]?.id ?: it.from
                it.copy(to = newTo, from = newFrom)
            }
            .distinct() // remove links that start pointing to the same beans
            .filter { it.to != it.from } // remove self linked beans

    return Topology(beanRefs, beanLinks, this.partitionsCount)
}