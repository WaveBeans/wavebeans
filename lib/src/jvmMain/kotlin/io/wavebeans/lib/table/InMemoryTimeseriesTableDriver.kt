package io.wavebeans.lib.table

import io.wavebeans.lib.TimeMeasure
import io.wavebeans.lib.s
import mu.KotlinLogging
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.reflect.KClass


/**
 * Basic implementation for in-memory [TimeseriesTableDriver], internally represented as a [java.util.Deque].
 *
 * Intended to use for small amounts of data as has no optimization and is doing thing not very effectively.
 * Basically all queries perform a full scan of the collection every time.
 *
 * Also, each element has quite an overhead of an object container and time marker, so keeping small
 * object like [io.wavebeans.lib.Sample]s leads to reasonable memory consumption.
 *
 * All these constraints should be kept in mind while using this implementation. It's not bad overall.
 * As pretty much everything out there, it should be used correctly.
 */
actual class InMemoryTimeseriesTableDriver<T : Any> actual constructor(
    override val tableName: String,
    override val tableType: KClass<*>,
    private val retentionPolicy: TableRetentionPolicy,
    private val automaticCleanupEnabled: Boolean
) : TimeseriesTableDriver<T> {

    companion object {
        private val log = KotlinLogging.logger { }
        private val scheduledExecutor =
            Executors.newSingleThreadScheduledExecutor { Thread(it, "InMemoryTimeseriesTableDriverThread") }

        init {
            Runtime.getRuntime().addShutdownHook(Thread {
                log.info { "Closing executor" }
                scheduledExecutor.shutdown()
                if (!scheduledExecutor.awaitTermination(5000, TimeUnit.MILLISECONDS)) {
                    scheduledExecutor.shutdownNow()
                }
                log.info { "Closed executor" }
            })
        }
    }


    override val sampleRate: Float
        get() = sampleRateValue[0]
            .let { if (it < 0) throw IllegalStateException("Sample rate value is not initialized yet") else it }


    private val _table = ConcurrentLinkedDeque<Item<T>>()


    private var cleanUpTask: ScheduledFuture<*>? = null
    private val sampleRateValue: FloatArray = FloatArray(1) { Float.NEGATIVE_INFINITY }
    private val isFinished = AtomicBoolean(false)

    override fun init(sampleRate: Float) {
        sampleRateValue[0] = sampleRate
        log.debug { "[$this] Initializing driver" }
        if (cleanUpTask == null && automaticCleanupEnabled) {
            log.debug { "[$this] Setting cleanup task" }
            cleanUpTask = scheduledExecutor.scheduleAtFixedRate({ performCleanup() }, 0, 5000, TimeUnit.MILLISECONDS)
        }
    }

    override fun finishStream() {
        log.debug { "[$this] Finishing stream" }
        isFinished.set(true)
    }

    override fun isStreamFinished(): Boolean = isFinished.get()

    override fun reset() {
        log.debug { "[$this] Resetting driver" }
        _table.clear()
        isFinished.set(false)
    }

    /**
     * Performs the clean up. Return number of elements removed.
     */
    fun performCleanup(): Int {
        log.debug {
            "[$this] Performing cleanup according to policy $retentionPolicy. " +
                    "Table: first=${_table.peekFirst()}, last=${_table.peekLast()}, size=${_table.size}"
        }
        var removedCount = 0
        try {
            val maximumTimeMarker = _table.peekLast()?.timeMarker ?: return removedCount
            while (true) {
                val first = _table.peekFirst() ?: return removedCount

                if (!retentionPolicy.isRetained(first.timeMarker, maximumTimeMarker)) {
                    _table.removeFirst()
                    removedCount++
                } else {
                    log.debug { "[$this] Performed clean up. Removed $removedCount items" }
                    return removedCount
                }
            }
        } catch (e: Exception) {
            log.error(e) { "[$this] Can't perform table clean up" }
            return removedCount
        }
    }

    override fun put(time: TimeMeasure, value: T) {
        if (isStreamFinished()) throw IllegalStateException("[$this] The stream is already finished, you can't put any more data in it")
        val peekLast = _table.peekLast()
        if (peekLast != null && time < peekLast.timeMarker)
            throw IllegalStateException("[$this] Can't put item with time=$time, as older one exists: $peekLast")
        _table += Item(time, value)
    }

    override fun close() {
        log.debug { "[$this] Closing" }
        cleanUpTask?.cancel(false)
        cleanUpTask = null
        log.debug { "[$this] Closed" }
    }


    override fun firstMarker(): TimeMeasure? = _table.peekFirst()?.timeMarker

    override fun lastMarker(): TimeMeasure? = _table.peekLast()?.timeMarker

    override fun query(query: TableQuery): Sequence<T> {
        log.debug { "[$this] Running query $query" }
        return when (query) {
            is TimeRangeTableQuery -> {
                _table.asSequence()
                    .filter { it.timeMarker >= query.from }
                    .takeWhile { it.timeMarker < query.to }
                    .map { it.value }
            }

            is LastIntervalTableQuery -> {
                val to = _table.peekLast()?.timeMarker ?: 0.s
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

    override fun toString(): String {
        return "InMemoryTimeseriesTableDriver(tableName='$tableName', tableType=$tableType, retentionPolicy=$retentionPolicy, isFinished=$isFinished)"
    }

    internal actual val table: Deque<Item<T>> = object : Deque<Item<T>> {
        override fun peekFirst(): Item<T> = _table.peekFirst()

        override fun peekLast(): Item<T> = _table.peekLast()

        override val size: Int
            get() = _table.size

        override fun iterator(): Iterator<Item<T>> = _table.iterator()
    }
}