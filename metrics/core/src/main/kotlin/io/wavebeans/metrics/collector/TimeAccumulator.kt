package io.wavebeans.metrics.collector

data class TimeAccumulator(
        val count: Int,
        val time: Long
) {
    operator fun plus(another: TimeAccumulator): TimeAccumulator = TimeAccumulator(count + another.count, time + another.time)
}