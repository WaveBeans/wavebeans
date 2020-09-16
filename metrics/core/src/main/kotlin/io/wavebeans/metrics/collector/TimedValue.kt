package io.wavebeans.metrics.collector

/**
 * Creates [TimedValue] of the specified value
 *
 * @param timestamp the [TimedValue.timestamp]
 * @receiver the [TimedValue.value]
 */
infix fun <T> T.at(timestamp: Long): TimedValue<T> = TimedValue(timestamp, this)

/**
 * Keeps the time marked value of type [T].
 */
data class TimedValue<T>(
        /**
         * The unix timestamp in milliseconds.
         */
        val timestamp: Long,
        /**
         * The value of type [T]
         */
        val value: T
)