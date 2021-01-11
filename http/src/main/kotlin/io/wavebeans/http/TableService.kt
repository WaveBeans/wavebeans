package io.wavebeans.http

import io.javalin.Javalin
import io.wavebeans.lib.TimeMeasure
import io.wavebeans.lib.s
import io.wavebeans.lib.table.TableRegistry
import java.io.ByteArrayInputStream
import java.io.InputStream

fun Javalin.tableService(tableRegistry: TableRegistry) {
    val tableService = TableService(tableRegistry)

    get("/table/:tableName/last") { context ->
        val tableName: String = context.requiredPath("tableName") { it }
        val interval: TimeMeasure = context.requiredQuery("interval") { TimeMeasure.parseOrNull(it) }

        if (!tableService.exists(tableName)) throw NotFoundException("$tableName is not found")

        val stream = tableService.last(tableName, interval)
        context.result(stream)

    }
    get("/table/:tableName/timeRange") { context ->
        val tableName: String = context.requiredPath("tableName") { it }
        val from: TimeMeasure = context.requiredQuery("from") { TimeMeasure.parseOrNull(it) }
        val to: TimeMeasure = context.requiredQuery("to") { TimeMeasure.parseOrNull(it) }

        if (!tableService.exists(tableName)) throw NotFoundException("$tableName is not found")

        val stream = tableService.timeRange(tableName, from, to)
        context.result(stream)
    }
}

class TableService(private val tableRegistry: TableRegistry) {

    fun exists(tableName: String): Boolean = tableRegistry.exists(tableName)

    fun last(tableName: String, interval: TimeMeasure): InputStream =
        if (tableRegistry.exists(tableName)) {
            val table = tableRegistry.byName<Any>(tableName)
            JsonBeanStreamReader(
                stream = table.last(interval),
                sampleRate = table.sampleRate,
                offset = table.lastMarker() ?: 0.s
            )
        } else {
            ByteArrayInputStream(ByteArray(0))
        }


    fun timeRange(tableName: String, from: TimeMeasure, to: TimeMeasure): InputStream =
        if (tableRegistry.exists(tableName)) {
            val table = tableRegistry.byName<Any>(tableName)
            JsonBeanStreamReader(
                stream = table.timeRange(from, to),
                sampleRate = table.sampleRate,
                offset = from
            )
        } else {
            ByteArrayInputStream(ByteArray(0))
        }
}
