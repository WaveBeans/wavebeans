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

    fun deserialize(query: String): TableQuery = json.parse(PolymorphicSerializer(TableQuery::class), query)

    fun serialize(query: TableQuery, json: Json = this.json): String = json.stringify(PolymorphicSerializer(TableQuery::class), query)
}