package io.wavebeans.metrics.collector

/**
 * List that keeps the values in chronological order and accumulates them into `granulars`
 * if the [granularValueInMs] is greater than 0. Granular is accumulated value for a period of time, the accumulation
 * is performed with the specifed function [accumulator].
 *
 * The current implementation is based on [ArrayList] type, the thread safety is guaranteed by synchronized methods. It is not
 * recommended to use that implementation to store a lot of values, please consider always using [granularValueInMs] and clean
 * up the list by [TimeseriesList.removeAllBefore] or [TimeseriesList.leaveOnlyLast] on time, it doesn't perform
 * any operation asynchronously.
 */
class TimeseriesList<T : Any>(
        private val granularValueInMs: Long = 60000,
        private val accumulator: (T, T) -> T
) {

    @Volatile
    private var timeseries = ArrayList<TimedValue<T>>()

    @Volatile
    private var lastValueTimestamp: Long = -1

    @Volatile
    private var lastValue: T? = null

    /**
     * Resets the values by creating a new list of values.
     */
    @Synchronized
    fun reset() {
        timeseries = ArrayList()
        lastValueTimestamp = -1
        lastValue = null
    }

    /**
     * Appends element to the list. Automatically accumulates it to the current granular or seal the current
     * granular if the time marker is outside of the desired granular and start the new one with specified value.
     *
     * @param v the value to append.
     * @param now if you want to override time marker for the value, by default it is [System.currentTimeMillis].
     *            The time marker for current implementation should be in future.
     *
     * @return true if the value was appended, or false otherwise. False is returned in the case if the time marker
     *              of latest stored value is older than the time marker of the value you're trying to append.
     */
    @Synchronized
    fun append(v: T, now: Long = System.currentTimeMillis()): Boolean {
        if (now < lastValueTimestamp) return false

        if (granularValueInMs > 0) {
            lastValue = lastValue?.let {
                val lastBucket = lastValueTimestamp / granularValueInMs
                val currentBucket = now / granularValueInMs
                if (currentBucket > lastBucket) {
                    timeseries.add(TimedValue(lastBucket * granularValueInMs, it))
                    v
                } else {
                    accumulator(it, v)
                }
            } ?: v
            lastValueTimestamp = now
        } else {
            timeseries.add(TimedValue(now, v))
        }
        return true
    }

    /**
     * Leaves only values in last [intervalMs] from [now] in the the list, everything outside of
     * interval [ [now] - [intervalMs], [now] ] is removed. That includes non-sealed granular if applicable.
     * Follow [append] to get sense of granular sealing process.
     *
     * @param intervalMs interval in milliseconds to leave.
     * @param now the time marker to use as a current momement, by default [System.currentTimeMillis].
     */
    @Synchronized
    fun leaveOnlyLast(intervalMs: Long, now: Long = System.currentTimeMillis()) {
        val lastMarker = now - intervalMs
        timeseries.removeIf { it.timestamp < lastMarker }
        if (lastValueTimestamp < lastMarker) {
            lastValueTimestamp = -1
            lastValue = null
        }
    }

    /**
     * Removes all values before [before] time marker and returns removed values. That includes the latest
     * non-sealed granular if applicable. Follow [append] to get sense of granular sealing process.
     *
     * The values returned as time marked values [TimedValue]. Important points regarding time marker of the value:
     * * if the granular is sealed, the time marker of the granular is returned, which is for example for
     *   1 min granularity is always divisible by 60000 milliseconds.
     * * if the granular is not sealed, the time marker is the same as the latest appended value time marker.
     *
     * @param before the time marker to remove before (inclusive). That is unix timestamp in milliseconds.
     *               You may use [Long.MAX_VALUE] to always remove all stored values.
     *
     * @return the list of [TimedValue] of empty list if nothing is found.
     */
    @Synchronized
    fun removeAllBefore(before: Long): List<TimedValue<T>> {
        val i = timeseries.iterator()
        val removedValues = ArrayList<TimedValue<T>>()
        while (i.hasNext()) {
            val e = i.next()
            if (e.timestamp < before) {
                i.remove()
                removedValues += e
            } else {
                break // the timestamp is growing only, may cancel the loop
            }
        }
        if (lastValueTimestamp < before && lastValue != null) {
            val e = TimedValue(lastValueTimestamp, lastValue!!)
            removedValues += e
            lastValueTimestamp = -1
            lastValue = null
        }
        return removedValues
    }

    /**
     * Runs the aggregation over the interval and returns it as a singular value. Uses [accumulator] function to aggregate values.
     *
     * @param intervalMs interval to run aggregation over, in milliseconds.
     * @param now the time marker to use as a current momement, by default [System.currentTimeMillis].
     *
     * @return the aggregated value or `null` if no value stored over that period.
     */
    @Synchronized
    fun inLast(intervalMs: Long, now: Long = System.currentTimeMillis()): T? {
        val lastMarker = now - intervalMs
        val v = timeseries.asSequence()
                .dropWhile { it.timestamp < lastMarker }
                .map { it.value }
                .reduceOrNull { acc, v -> accumulator(acc, v) }
        return when {
            lastValue != null && lastMarker < lastValueTimestamp && v != null -> accumulator(v, lastValue!!)
            v == null -> lastValue
            else -> v
        }
    }

    /**
     * Merges the [values] into the current state respecting time markers of the specified values. The resulting state
     * has similar sealed as non-sealed granulars, but their values are augmented based on the provided values.
     * Follow [append] to get sense of granular sealing process.
     *
     * @param values the [Sequence] of the values to merge.
     */
    @Synchronized
    fun merge(values: Sequence<TimedValue<T>>) {
        val i = (timeseries + (
                lastValue?.let { listOf(TimedValue(lastValueTimestamp, it)) }
                        ?: emptyList()
                ))
                .iterator()
        val j = values.iterator()

        reset()
        var e1: TimedValue<T>? = null
        var e2: TimedValue<T>? = null
        while (true) {
            e1 = if (e1 == null && i.hasNext()) i.next() else e1
            e2 = if (e2 == null && j.hasNext()) j.next() else e2
            if (e1 == null && e2 == null) {
                break
            } else if (e1 != null && e2 == null) {
                append(e1.value, e1.timestamp)
                e1 = null
            } else if (e1 == null && e2 != null) {
                append(e2.value, e2.timestamp)
                e2 = null
            } else if (e1!!.timestamp < e2!!.timestamp) {
                append(e1.value, e1.timestamp)
                e1 = null
            } else { // (e1!!.first >= e2!!.first)
                append(e2.value, e2.timestamp)
                e2 = null
            }
        }
    }

    /**
     * Creates a full copy of the current state respecting sealed and non-sealed granulars.
     * Follow [append] to get sense of granular sealing process.
     *
     * The values returned as time marked values [TimedValue]. Important points regarding time marker of the value:
     * * if the granular is sealed, the time marker of the granular is returned, which is for example for
     *   1 min granularity is always divisible by 60000 milliseconds.
     * * if the granular is not sealed, the time marker is the same as the latest appended value time marker.
     */
    @Synchronized
    fun valuesCopy(): List<TimedValue<T>> {
        return ArrayList(timeseries) +
                (lastValue?.let { listOf(TimedValue(lastValueTimestamp, it)) } ?: emptyList())
    }
}
