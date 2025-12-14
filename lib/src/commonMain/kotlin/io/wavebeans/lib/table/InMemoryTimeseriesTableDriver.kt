package io.wavebeans.lib.table

import io.wavebeans.lib.TimeMeasure
import kotlin.reflect.KClass

internal data class Item<T : Any>(val timeMarker: TimeMeasure, val value: T)

interface Deque<T> {
    fun peekFirst(): T?
    fun peekLast() : T?
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
    override val tableName: String
    override val sampleRate: Float
    override val tableType: KClass<*>
    override fun init(sampleRate: Float)
    override fun reset()
    override fun put(time: TimeMeasure, value: T)
    override fun firstMarker(): TimeMeasure?
    override fun lastMarker(): TimeMeasure?
    override fun query(query: TableQuery): Sequence<T>
    override fun finishStream()
    override fun isStreamFinished(): Boolean
    override fun close()
}

