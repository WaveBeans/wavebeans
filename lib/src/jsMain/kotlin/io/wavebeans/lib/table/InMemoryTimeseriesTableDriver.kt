package io.wavebeans.lib.table

import io.wavebeans.lib.TimeMeasure
import kotlin.reflect.KClass

actual class InMemoryTimeseriesTableDriver<T : Any> actual constructor(
    actual override val tableName: String,
    actual override val tableType: KClass<*>,
    private val retentionPolicy: TableRetentionPolicy,
    private val automaticCleanupEnabled: Boolean
) : TimeseriesTableDriver<T> {
    actual override val sampleRate: Float
        get() = TODO("Not yet implemented")

    actual override fun init(sampleRate: Float) {
        TODO("Not yet implemented")
    }

    actual override fun reset() {
        TODO("Not yet implemented")
    }

    actual override fun firstMarker(): TimeMeasure? {
        TODO("Not yet implemented")
    }

    actual override fun lastMarker(): TimeMeasure? {
        TODO("Not yet implemented")
    }

    actual override fun query(query: TableQuery): Sequence<T> {
        TODO("Not yet implemented")
    }

    actual override fun finishStream() {
        TODO("Not yet implemented")
    }

    actual override fun isStreamFinished(): Boolean {
        TODO("Not yet implemented")
    }

    actual override fun put(time: TimeMeasure, value: T) {
        TODO("Not yet implemented")
    }

    actual override fun close() {
        TODO("Not yet implemented")
    }

    internal actual val table: Deque<Item<T>>
        get() = TODO("Not yet implemented")

}