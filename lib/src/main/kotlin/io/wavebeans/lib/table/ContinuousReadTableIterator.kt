package io.wavebeans.lib.table

import io.wavebeans.lib.TimeMeasure
import io.wavebeans.lib.ns
import io.wavebeans.lib.s
import mu.KotlinLogging
import java.lang.Thread.*
import java.util.*

/**
 * Implementation of iterator that continously reads the provided deque, assuming that someone from outside appends elements.
 *
 * Should be considered as quick and dirty specifically for [InMemoryTimeseriesTableDriver] and never used anywhere else.
 *
 * Regular deque iterator ends quickly when the deque is over, but that iterator allows to wait for the next element to
 * appear and continue reading from it. Though it is achieved by restarting iterator, which is not very efficient
 * as every time it starts to read from the very beginning of the collection skipping unneeded elements.
 * That approach going to be inefficient for long table-buffers.
 */
internal class ContinuousReadTableIterator<T : Any>(
        tableDriver: InMemoryTimeseriesTableDriver<T>,
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

    init {
        log.debug {
            "[$this:$iterator] Starting iterator and skipping values up to $from. " +
                    "Table: first=${table.peekFirst()}, last=${table.peekLast()}, size=${table.size}"
        }
    }

    override fun hasNext(): Boolean = true

    override fun next(): T {

        var e: Item<T>?

        do {
            e = if (iterator.hasNext()) iterator.next() else null

            if (e == null) {
                log.trace { "[$this:$iterator] Read $returned element, iterator got empty, waiting for a little bit and restarting it." }
                var i = 0
                do {
                    // TODO may check if table has changed its state to avoid creating iterator over and over again when there is no new elements
                    sleep(0)
                    iterator = table.iterator()
                    i++
                } while (!iterator.hasNext())
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
        } while (e == null)
        previousE = e

        if (perElementLog) log.trace { "[$this:$iterator] Element returning $e" }
        returned++
        return e.value
    }
}