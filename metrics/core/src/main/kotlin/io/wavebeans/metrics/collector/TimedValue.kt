package io.wavebeans.metrics.collector

infix fun <T> T.at(timestamp: Long): TimedValue<T> = TimedValue(timestamp, this)

data class TimedValue<T>(
        val timestamp: Long,
        val value: T
)