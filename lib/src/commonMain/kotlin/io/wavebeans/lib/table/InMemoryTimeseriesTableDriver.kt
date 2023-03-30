package io.wavebeans.lib.table

import io.wavebeans.lib.TimeMeasure
import kotlin.reflect.KClass

internal data class Item<T : Any>(val timeMarker: TimeMeasure, val value: T)

interface Deque<T> {
    fun peekFirst(): T
    fun peekLast() :T
    val size: Int
    fun iterator(): Iterator<T>

}

expect class InMemoryTimeseriesTableDriver<T : Any>(
    tableName: String,
    tableType: KClass<*>,
    retentionPolicy: TableRetentionPolicy,
    automaticCleanupEnabled: Boolean = true
) : TimeseriesTableDriver<T> {
    internal val table: Deque<Item<T>>
}

