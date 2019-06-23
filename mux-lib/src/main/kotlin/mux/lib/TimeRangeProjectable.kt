package mux.lib

import java.util.concurrent.TimeUnit

interface TimeRangeProjectable<O> {
    /**
     * Gets a projection of the object in the specified time range.
     *
     * @param start starting point of the projection in time units.
     * @param end ending point of the projection (including) in time units. Null if till the end
     * @param timeUnit the units the projection is defined in (i.e seconds, milliseconds, microseconds). TODO: replace TimeUnit with non-java.util.concurrent one
     *
     * @return the projection of specific time interval
     */
    fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): O

}