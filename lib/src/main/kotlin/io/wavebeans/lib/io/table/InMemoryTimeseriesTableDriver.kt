package io.wavebeans.lib.io.table

import io.wavebeans.lib.*
import mu.KotlinLogging
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class InMemoryTimeseriesTableDriver<T : Any>(
        private val tableName: String,
        private val retentionPolicy: TableRetentionPolicy
) : TimeseriesTableDriver<T> {

    companion object {
        private val log = KotlinLogging.logger { }
    }

    private data class Item<T : Any>(val timeMarker: TimeMeasure, val value: T)

    private val table = ConcurrentLinkedDeque<Item<T>>()

    private val scheduledExecutor = Executors.newSingleThreadScheduledExecutor { Thread(it, this.toString()) }
            .also { it.scheduleAtFixedRate({ performCleanup() }, 0, 5000, TimeUnit.MILLISECONDS) }


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

    override fun last(interval: TimeMeasure): BeanStream<T> {
        log.debug { "[$this] Getting last $interval" }
        val peekLast = table.peekLast()
        val lastTimeMarker = peekLast?.timeMarker ?: 0.ns
        val firstTimeMarker = peekLast?.let { it.timeMarker - interval } ?: 0.ns
        return timeRange(firstTimeMarker, lastTimeMarker)
    }

    override fun timeRange(from: TimeMeasure, to: TimeMeasure): BeanStream<T> {
        log.debug { "[$this] Getting time range from $from to $to" }
        return object : BeanStream<T> {

            override fun asSequence(sampleRate: Float): Sequence<T> {
                return table.asSequence()
                        .filter { it.timeMarker >= from }
                        .takeWhile { it.timeMarker < to }
                        .map { it.value }
            }

            override fun inputs(): List<AnyBean> = throw UnsupportedOperationException("not required")

            override val parameters: BeanParams
                get() = throw UnsupportedOperationException("not required")
        }
    }

    override fun close() {
        log.debug { "[$this] Shutting down..." }
        scheduledExecutor.shutdown()
        if (!scheduledExecutor.awaitTermination(5000, TimeUnit.MILLISECONDS)) {
            log.debug { "[$this] Can't wait to shutdown. Forcing..." }
            scheduledExecutor.shutdownNow()
        }
        log.debug { "[$this] Closed" }
    }

    override fun toString(): String {
        return "InMemoryTimeseriesTableDriver($tableName)"
    }


}