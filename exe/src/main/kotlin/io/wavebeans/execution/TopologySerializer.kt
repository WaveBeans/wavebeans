package io.wavebeans.execution

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.modules.SerializersModule

object TopologySerializer {

    private val log = KotlinLogging.logger{}

    val paramsModule = SerializersModule {
        beanParams()
        tableQuery()
    }

    private val json = jsonCompact(paramsModule)
    private val pretty = jsonPretty(paramsModule)

    fun deserialize(topology: String): Topology = try {
        json.decodeFromString(Topology.serializer(), topology)
    } catch (e: Exception) {
        log.warn(e) { "Can't deserialize the topology:\n $topology" }
        throw e
    }

    fun serialize(topology: Topology): String = try {
        json.encodeToString(Topology.serializer(), topology)
    } catch (e: Exception) {
        log.warn(e) { "Can't serialize the topology:\n $topology" }
        throw e
    }
    fun serializePretty(topology: Topology): String = pretty.encodeToString(Topology.serializer(), topology)
}