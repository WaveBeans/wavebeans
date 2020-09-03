package io.wavebeans.execution

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

object TopologySerializer {

    val paramsModule = SerializersModule {
        beanParams()
        tableQuery()
    }

    private val json = jsonCompact(paramsModule)

    fun deserialize(topology: String): Topology = json.decodeFromString(Topology.serializer(), topology)

    fun serialize(topology: Topology, json: Json = this.json): String = json.encodeToString(Topology.serializer(), topology)
}