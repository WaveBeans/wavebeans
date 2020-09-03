package io.wavebeans.execution

import io.wavebeans.lib.table.TableQuery
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

object TableQuerySerializer {
    val paramsModule = SerializersModule {
        tableQuery()
    }

    private val json = jsonCompact(paramsModule)

    fun deserialize(query: String): TableQuery = json.decodeFromString(PolymorphicSerializer(TableQuery::class), query)

    fun serialize(query: TableQuery, json: Json = this.json): String = json.encodeToString(PolymorphicSerializer(TableQuery::class), query)
}