package io.wavebeans.lib.table

import io.wavebeans.lib.BeanStream
import io.wavebeans.lib.TimeMeasure
import java.io.Closeable

interface TimeseriesTableDriver<T : Any> : Closeable {

    val tableName: String

    /**
     * Initializes the driver.
     */
    fun init()

    /**
     * Resets the driver state.
     */
    fun reset()

    /**
     * Puts the new values into the table. Depending on implementation may require the [time]
     * to be always greater than [lastMarker].
     *
     * @param time the time marker of the value.
     * @param value the value associated with the marker.
     */
    fun put(time: TimeMeasure, value: T)

    /**
     * Gets the values as stream within last interval in the tables.
     * The resulting stream is finite.
     *
     * @param interval the measure of the interval to get values from.
     *
     * @return a stream values laying within specified interval
     */
    fun last(interval: TimeMeasure): BeanStream<T> {
        return TableDriverInput(TableDriverStreamParams(tableName, LastIntervalTableQuery(interval)))
    }

    /**
     * Gets the values as stream which has time markers between [from] (inclusvie) and [to] (exclusive).
     * The resulting stream is finite.
     *
     * @param from starting marker of the returning interval, inclusive.
     * @param to ending marker of the returning interval, exclusive
     *
     * @return a stream values laying within specified interval
     */
    fun timeRange(from: TimeMeasure, to: TimeMeasure): BeanStream<T> {
        return TableDriverInput(TableDriverStreamParams(tableName, TimeRangeTableQuery(from, to)))
    }

    /**
     * Gets the first time marker of the table.
     *
     * @return the value of the first marker or null if table is empty.
     */
    fun firstMarker(): TimeMeasure?

    /**
     * Gets the last time marker of the table
     */
    fun lastMarker(): TimeMeasure?

    /**
     * Runs the query on the table returning the result as sequence.
     *
     * @param query the query to run
     *
     * @return the result of the query as sequence, can be empty sequence if there is nothing to return.
     */
    fun query(query: TableQuery): Sequence<T>
}

