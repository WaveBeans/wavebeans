package mux.lib.execution

import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.Serializable
import mux.lib.Bean
import mux.lib.BeanParams
import mux.lib.io.StreamOutput

@Serializable
data class BeanRef(
        val id: Int,
        val type: String,
        @Serializable(with = PolymorphicSerializer::class) val params: BeanParams,
        val partition: Int = 0
)

@Serializable
data class BeanLink(
        val from: Int,
        val to: Int,
        val order: Int = 0
)

@Serializable
data class Topology(
        val refs: List<BeanRef>,
        val links: List<BeanLink>
) {
    companion object {

        internal fun build(outputs: List<StreamOutput<*, *>>, idResolver: IdResolver): Topology {
            val refs = mutableListOf<BeanRef>()
            val links = mutableListOf<BeanLink>()
            fun iterateOverNodes(node: Bean<*, *>, sourceNodeId: Int?, order: Int) {
                val id = idResolver.id(node)

                refs += BeanRef(id, node::class.qualifiedName!!, node.parameters)

                if (sourceNodeId != null)
                    links += BeanLink(sourceNodeId, id, order)

                node.inputs().forEachIndexed { idx, n -> iterateOverNodes(n, id, idx) }
            }

            outputs.forEach {
                iterateOverNodes(it, null, 0)
            }

            return Topology(refs, links.distinct())
        }
    }
}

fun List<StreamOutput<*, *>>.buildTopology(idResolver: IdResolver = IntSequenceIdResolver()): Topology = Topology.build(this, idResolver)

interface IdResolver {
    fun id(node: Bean<*, *>): Int
}

internal class IntSequenceIdResolver : IdResolver {

    private var idSeq = 0
    private val nodesRef = mutableMapOf<Int, Bean<*, *>>()

    override fun id(node: Bean<*, *>): Int {
        val id = nodesRef.entries.firstOrNull { it.value == node }?.key ?: ++idSeq
        nodesRef[id] = node
        return id
    }

}
