package mux.lib.execution

import kotlinx.serialization.Serializable
import mux.lib.BeanParams
import mux.lib.SinkBean
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
            .mapValues { it.value.first() }

    val strokes = mutableListOf<List<BeanRef>>()

    fun buildStrokes(bean: BeanRef, currentStroke: List<BeanRef>) {
        val linksToBean = beanLinksTo[bean.id] ?: emptyList()
        val linksFromBean = beanLinksFrom[bean.id] ?: emptyList()
        when {
            linksFromBean.size > 1 -> { // split, stroke finished on this element
                strokes += currentStroke + bean
                linksFromBean.forEach { buildStrokes(beans.getValue(it.to), emptyList()) }
            }
            linksToBean.size > 1 -> { // merge operation, stroke finished on previous element
                strokes += currentStroke
                linksToBean.forEach { buildStrokes(beans.getValue(it.to), listOf(bean)) }
            }
            linksFromBean.size == 1 -> { // stroke continues
                buildStrokes(beans.getValue(linksFromBean.first().to), currentStroke + bean)
            }
            linksFromBean.isEmpty() && currentStroke.isNotEmpty() -> { // finish stroke
                strokes += currentStroke + bean
            }
        }
    }

    this.refs
            .filter { Class.forName(it.type).kotlin.isSubclassOf(SinkBean::class) }
            .forEach {
                buildStrokes(it, emptyList())
            }

    // create new bean structure
    val replacedBeans = mutableMapOf<Int, BeanRef>()
    val beanRefs = strokes.map { stroke ->
        if (stroke.size > 1) {
            val internalIds = stroke.map { it.id }.toSet()
            val beanGroupRef = BeanRef.create(idResolver.id(), BeanGroup::class, BeanGroupParams(
                    stroke,
                    this.links.filter { it.from in internalIds && it.to in internalIds }
            ))
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

    return Topology(beanRefs, beanLinks)
}