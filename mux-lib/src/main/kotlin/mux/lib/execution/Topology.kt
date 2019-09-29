package mux.lib.execution

import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.Serializable
import mux.lib.MuxNode
import mux.lib.MuxParams
import mux.lib.io.StreamOutput

@Serializable
data class MuxNodeRef(
        val id: Int,
        val type: String,
        @Serializable(with = PolymorphicSerializer::class) val params: MuxParams
)

@Serializable
data class MuxNodeLink(
        val from: Int,
        val to: Int
)

@Serializable
data class Topology(
        val refs: List<MuxNodeRef>,
        val links: List<MuxNodeLink>
) {
    companion object {

        internal fun build(outputs: List<StreamOutput<*, *>>, idResolver: IdResolver): Topology {
            val refs = mutableListOf<MuxNodeRef>()
            val links = mutableListOf<MuxNodeLink>()
            fun iterateOverNodes(node: MuxNode<*, *>, sourceNodeId: Int?) {
                val id = idResolver.id(node)

                refs += MuxNodeRef(id, node::class.qualifiedName!!, node.parameters)

                if (sourceNodeId != null)
                    links += MuxNodeLink(sourceNodeId, id)

                node.inputs().forEach { iterateOverNodes(it, id) }
            }

            outputs.forEach {
                iterateOverNodes(it, null)
            }

            return Topology(refs, links)
        }
    }
}

fun List<StreamOutput<*, *>>.buildTopology(idResolver: IdResolver = IntSequenceIdResolver()): Topology = Topology.build(this, idResolver)

interface IdResolver {
    fun id(node: MuxNode<*, *>): Int
}

internal class IntSequenceIdResolver : IdResolver {

    private var idSeq = 0
    private val nodesRef = mutableMapOf<Int, MuxNode<*, *>>()

    override fun id(node: MuxNode<*, *>): Int {
        val id = nodesRef.entries.firstOrNull { it.value == node }?.key ?: ++idSeq
        nodesRef[id] = node
        return id
    }

}
