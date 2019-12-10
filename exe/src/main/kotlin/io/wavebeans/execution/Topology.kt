package io.wavebeans.execution

import io.wavebeans.lib.AnyBean
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.Serializable
import io.wavebeans.lib.Bean
import io.wavebeans.lib.BeanParams
import io.wavebeans.lib.io.StreamOutput
import kotlin.reflect.KClass

@Serializable
data class BeanRef(
        val id: Int,
        val type: String,
        @Serializable(with = PolymorphicSerializer::class) val params: BeanParams,
        val partition: Int = 0
) {
    companion object {
        fun create(id: Int, type: KClass<out Any>, parameters: BeanParams, partition: Int = 0): BeanRef =
                BeanRef(id, type.qualifiedName!!, parameters, partition)
    }
}

@Serializable
data class BeanLink(
        val from: Int,
        val to: Int,
        val fromPartition: Int = 0,
        val toPartition: Int = 0,
        val order: Int = 0
)

@Serializable
data class Topology(
        val refs: List<BeanRef>,
        val links: List<BeanLink>,
        val partitionsCount: Int = 1
) {
    companion object {

        internal fun build(outputs: List<StreamOutput<*>>, idResolver: IdResolver): Topology {
            val refs = mutableListOf<BeanRef>()
            val links = mutableListOf<BeanLink>()
            fun iterateOverNodes(node: AnyBean, sourceNodeId: Int?, order: Int) {
                val id = idResolver.id(node)

                refs += BeanRef.create(id, node::class, node.parameters)

                if (sourceNodeId != null)
                    links += BeanLink(
                            from = sourceNodeId,
                            to = id,
                            order = order
                    )

                node.inputs().forEachIndexed { idx, n -> iterateOverNodes(n, id, idx) }
            }

            outputs.forEach {
                iterateOverNodes(it, null, 0)
            }

            return Topology(refs.distinctBy { it.id }, links.distinct())
        }
    }
}

fun List<StreamOutput<*>>.buildTopology(idResolver: IdResolver = IntSequenceIdResolver()): Topology = Topology.build(this, idResolver)

interface IdResolver {
    fun id(bean: AnyBean): Int
}

internal class IntSequenceIdResolver : IdResolver {

    private var idSeq = 0
    private val nodesRef = mutableMapOf<Int, Bean<*>>()

    override fun id(bean: AnyBean): Int {
        val id = nodesRef.entries.firstOrNull { it.value == bean }?.key ?: ++idSeq
        nodesRef[id] = bean
        return id
    }

}
