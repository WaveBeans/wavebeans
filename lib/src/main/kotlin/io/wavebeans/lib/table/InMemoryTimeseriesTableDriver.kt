package io.wavebeans.lib.table

import io.wavebeans.lib.TimeMeasure
import io.wavebeans.lib.s
import mu.KotlinLogging
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class InMemoryTimeseriesTableDriver<T : Any>(
        override val tableName: String,
        private val retentionPolicy: TableRetentionPolicy
) : TimeseriesTableDriver<T> {

    companion object {
        private val log = KotlinLogging.logger { }
        private val scheduledExecutor = Executors.newSingleThreadScheduledExecutor { Thread(it, "InMemoryTimeseriesTableDriver") }

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

    private data class Item<T : Any>(val timeMarker: TimeMeasure, val value: T)

    private val table = ConcurrentLinkedDeque<Item<T>>()

    private var cleanUpTask: ScheduledFuture<*>? = null

    override fun init() {
        log.debug { "[$this] Initializing driver" }
        if (cleanUpTask != null) {
            cleanUpTask = scheduledExecutor.scheduleAtFixedRate({ performCleanup() }, 0, 5000, TimeUnit.MILLISECONDS)
        }
    }

    override fun reset() {
        log.debug { "[$this] Resetting driver" }
        table.clear()
    }

    fun performCleanup() {
        log.debug { "[$this] Performing cleanup according to policy $retentionPolicy" }
        try {
            val maximumTimeMarker = table.peekLast()?.timeMarker ?: return
            var removedCount = 0
            while (true) {
                val first = table.peekFirst() ?: return

                if (!retentionPolicy.isRetained(first.timeMarker, maximumTimeMarker)) {
                    table.removeFirst()
                    removedCount++
                } else {
                    log.debug { "[$this] Performed clean up. Removed $removedCount items" }
                    return
                }
            }
        } catch (e: Exception) {
            log.error(e) { "[$this] Can't perform table clean up" }
        }
    }

    override fun put(time: TimeMeasure, value: T) {
        val peekLast = table.peekLast()
        if (peekLast != null && time < peekLast.timeMarker)
            throw IllegalStateException("Can't put item with time=$time, as older one exists: $peekLast")
        table += Item(time, value)
    }

    override fun close() {
        log.debug { "[$this] Closing" }
        cleanUpTask?.cancel(false)
        cleanUpTask = null
        log.debug { "[$this] Closed" }
    }

    override fun toString(): String {
        return "InMemoryTimeseriesTableDriver($tableName)"
    }

    override fun firstMarker(): TimeMeasure? = table.peekFirst()?.timeMarker

    override fun lastMarker(): TimeMeasure? = table.peekLast()?.timeMarker

    override fun query(query: TableQuery): Sequence<T> {
        log.debug { "[$this] Running query $query" }
        return when (query) {
            is TimeRangeTableQuery -> {
                table.asSequence()
                        .filter { it.timeMarker >= query.from }
                        .takeWhile { it.timeMarker < query.to }
                        .map { it.value }
            }
            is LastIntervalTableQuery -> {
                val to = table.peekLast()?.timeMarker ?: 0.s
                val from = to - query.interval
                table.asSequence()
                        .filter { it.timeMarker >= from }
                        .takeWhile { it.timeMarker < to }
                        .map { it.value }
            }
            else -> throw IllegalStateException("$query is not supported")
        }
    }
}