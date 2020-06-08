package io.wavebeans.lib.table

import io.wavebeans.lib.TimeMeasure
import io.wavebeans.lib.ns
import io.wavebeans.lib.s
import mu.KotlinLogging
import java.lang.Thread.*
import kotlin.NoSuchElementException

/**
 * Implementation of iterator that continously reads the provided deque, assuming that someone from outside appends elements.
 *
 * Should be considered as quick and dirty specifically for [InMemoryTimeseriesTableDriver] and never used anywhere else.
 *
 * Regular deque iterator ends quickly when the deque is over, but that iterator allows to wait for the next element to
 * appear and continue reading from it. Though it is achieved by restarting iterator, which is not very efficient
 * as every time it starts to read from the very beginning of the collection skipping unneeded elements.
 * That approach going to be inefficient for long table-buffers.
 *
 * Supports finishing stream by the table, so when [TimeseriesTableDriver.isStreamFinished] returns true it'll finishes
 * gracefully as well returning everything to the end of the table respecting the offset.
 */
internal class ContinuousReadTableIterator<T : Any>(
        val tableDriver: InMemoryTimeseriesTableDriver<T>,
        offset: TimeMeasure
) : Iterator<T> {

    companion object {
        private val log = KotlinLogging.logger { }
    }

    internal val table = tableDriver.table
    internal var from = (table.peekLast()?.timeMarker ?: 0.s) - offset
    internal var skippedToFrom = false
    internal var skipped = 0L
    internal var returned = 0L
    internal var previousE: Item<T>? = null
    internal var iterator = table.iterator()
    internal var perElementLog = false
    internal var streamIsOver = false
    internal var nextElement: T? = null

    init {
        log.debug {
            "[$this:$iterator] Starting iterator and skipping values up to $from. " +
                    "Table: first=${table.peekFirst()}, last=${table.peekLast()}, size=${table.size}"
        }
    }

    override fun hasNext(): Boolean = !streamIsOver || nextElement != null || advance(waitForNextElement = false) != null

    override fun next(): T {
        if (nextElement == null && advance(waitForNextElement = !streamIsOver) == null) throw NoSuchElementException("No more elements to read")
        val element = nextElement!!
        nextElement = null
        return element
    }

    private fun advance(waitForNextElement: Boolean): T? {
        var e: Item<T>?

        do {
            if (tableDriver.isStreamFinished()) streamIsOver = true
            e = if (iterator.hasNext()) iterator.next() else null

            if (e == null && streamIsOver) break

            if (e == null) {
                log.trace { "[$this:$iterator] Read $returned element, iterator got empty, waiting for a little bit and restarting it." }
                var i = 0
                do {
                    // TODO may check if table has changed its state to avoid creating iterator over and over again when there is no new elements
                    sleep(0) // sleep is essential here to be able to get interrupted
                    iterator = table.iterator()
                    i++
                } while (!streamIsOver && waitForNextElement && !iterator.hasNext())
                log.trace {
                    "[$this:$iterator] Created non-empty iterator in $i attempts." +
                            "Table: first=${table.peekFirst()}, last=${table.peekLast()}, size=${table.size}"
                }
                val prevE = previousE
                if (prevE != null && skippedToFrom) {
                    from = prevE.timeMarker + 1.ns // a little more than last already returned element
                    log.trace { "[$this:$iterator] New `from` value: $from." }
                    skippedToFrom = false
                }
            } else if (!skippedToFrom) {
                if (e.timeMarker >= from) {
                    log.trace { "[$this:$iterator] Skipped elements up to $from. Skipped $skipped" }
                    skippedToFrom = true
                    skipped = 0
                } else {
                    skipped++
                    if (perElementLog) log.trace { "[$this:$iterator] Skipping elements $e. Skipped=$skipped" }
                    e = null
                }
            }
        } while (waitForNextElement && e == null)

        nextElement = e?.let {
            previousE = it
            if (perElementLog) log.trace { "[$this:$iterator] Element returning $it" }
            returned++
            it.value
        }
        return nextElement
    }
}