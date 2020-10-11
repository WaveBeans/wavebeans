package io.wavebeans.metrics.collector

/**
 * Accumulate time measures, the [count] represents the the number of measures made, and the [time]
 * overall accumulated time, mainly in ms but it is undefined in this context.
 */
data class TimeAccumulator(
        val count: Int,
        val time: Long
) {

    /**
     * Sums up the two values -- count and time separately.
     */
    operator fun plus(another: TimeAccumulator): TimeAccumulator = TimeAccumulator(count + another.count, time + another.time)
}