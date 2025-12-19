package io.wavebeans.lib.table

import io.wavebeans.lib.TimeMeasure
import io.wavebeans.lib.s
import kotlin.reflect.KClass

actual class InMemoryTimeseriesTableDriver<T : Any> actual constructor(
    actual override val tableName: String,
    actual override val tableType: KClass<*>,
    private val retentionPolicy: TableRetentionPolicy,
    private val automaticCleanupEnabled: Boolean
) : TimeseriesTableDriver<T> {

    private var sampleRateValue: Float = Float.NaN
    private var isFinished: Boolean = false
    private val _table = mutableListOf<Item<T>>()

    actual override val sampleRate: Float
        get() = if (sampleRateValue.isNaN()) throw IllegalStateException("Sample rate value is not initialized yet") else sampleRateValue

    actual override fun init(sampleRate: Float) {
        sampleRateValue = sampleRate
    }

    actual override fun reset() {
        _table.clear()
        isFinished = false
    }

    actual override fun firstMarker(): TimeMeasure? = _table.firstOrNull()?.timeMarker

    actual override fun lastMarker(): TimeMeasure? = _table.lastOrNull()?.timeMarker

    actual override fun query(query: TableQuery): Sequence<T> {
        return when (query) {
            is TimeRangeTableQuery -> {
                _table.asSequence()
                    .filter { it.timeMarker >= query.from }
                    .takeWhile { it.timeMarker < query.to }
                    .map { it.value }
            }

            is LastIntervalTableQuery -> {
                val to = lastMarker() ?: 0.s
                val from = to - query.interval
                _table.asSequence()
                    .filter { it.timeMarker > from }
                    .takeWhile { it.timeMarker <= to }
                    .map { it.value }
            }

            is ContinuousReadTableQuery -> ContinuousReadTableIterator(this, query.offset).asSequence()
            else -> throw IllegalStateException("$query is not supported")
        }
    }

    actual override fun finishStream() {
        isFinished = true
    }

    actual override fun isStreamFinished(): Boolean = isFinished

    actual override fun put(time: TimeMeasure, value: T) {
        if (isStreamFinished()) throw IllegalStateException("The stream is already finished, you can't put any more data in it")
        val peekLast = lastMarker()
        if (peekLast != null && time < peekLast)
            throw IllegalStateException("Can't put item with time=$time, as newer one exists: $peekLast")
        _table.add(Item(time, value))
        if (automaticCleanupEnabled) {
            performCleanup()
        }
    }

    private fun performCleanup(): Int {
        var removedCount = 0
        val maximumTimeMarker = lastMarker() ?: return 0
        while (_table.isNotEmpty()) {
            val first = _table.first()
            if (!retentionPolicy.isRetained(first.timeMarker, maximumTimeMarker)) {
                _table.removeAt(0)
                removedCount++
            } else {
                break
            }
        }
        return removedCount
    }

    actual override fun close() {
    }

    internal actual val table: Deque<Item<T>> = object : Deque<Item<T>> {
        override fun peekFirst(): Item<T>? = _table.firstOrNull()

        override fun peekLast(): Item<T>? = _table.lastOrNull()

        override val size: Int
            get() = _table.size

        override fun iterator(): Iterator<Item<T>> = _table.iterator()
    }
}