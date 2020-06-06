package io.wavebeans.lib

import kotlinx.serialization.Serializable
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.*

fun TimeUnit.abbreviation(): String {
    return when (this) {
        NANOSECONDS -> "ns"
        MICROSECONDS -> "us"
        MILLISECONDS -> "ms"
        SECONDS -> "s"
        MINUTES -> "m"
        HOURS -> "h"
        DAYS -> "d"
    }
}

/**
 * Creates a new instance of [TimeMeasure] based on the number. The resulting unit is nanoseconds.
 */
val Number.ns
    get() = TimeMeasure(this.toLong(), NANOSECONDS)

/**
 * Creates a new instance of [TimeMeasure] based on the number. The resulting unit is microseconds.
 */
val Number.us
    get() = TimeMeasure(this.toLong(), MICROSECONDS)

/**
 * Creates a new instance of [TimeMeasure] based on the number. The resulting unit is milliseconds.
 */
val Number.ms
    get() = TimeMeasure(this.toLong(), MILLISECONDS)

/**
 * Creates a new instance of [TimeMeasure] based on the number. The resulting unit is seconds.
 */
val Number.s
    get() = TimeMeasure(this.toLong(), SECONDS)

/**
 * Creates a new instance of [TimeMeasure] based on the number. The resulting unit is minutes.
 */
val Number.m
    get() = TimeMeasure(this.toLong(), MINUTES)

/**
 * Creates a new instance of [TimeMeasure] based on the number. The resulting unit is hours.
 */
val Number.h
    get() = TimeMeasure(this.toLong(), HOURS)

/**
 * Creates a new instance of [TimeMeasure] based on the number. The resulting unit is days.
 */
val Number.d
    get() = TimeMeasure(this.toLong(), DAYS)

/**
 * Describes intervals of time with granularity from nanosecinds to days. Keep only integer part,
 * so for example 1.5 days can de defined only by 36 hours.
 *
 * Intervals are compared between each other by length taking into account units.
 *
 * Can be parsed from string representation. Consists of positive or negative number or zero and unit with no delimiter, i.e. `1s`, `-5d` and so on.
 *
 * Number formats, examples:
 *  * `100`, `100L` -- any integer and long values.
 *  * double `100.2` or as float `100.2f`, however will be rounded down to `100`
 *  * `1e2` -- the value is double, but has only integer part so will be interpreted as `100`
 *  * `1.2e2` -- the value is double as well, but keeping in mind mantias the value will be interpreted as `1.2 * 100 = 120.0 = 120`
 *  * `-100`, `-1.2` -- all negatives are also supported.
 *
 * The second part is time unit which is 1 or 2 latin symbols, case doesn't matter:
 *  * `ns` -- nanoseconds
 *  * `us` -- microseconds
 *  * `ms` -- milliseconds
 *  * `s` -- seconds
 *  * `m` -- minutes
 *  * `h` -- hours
 *  * `d` -- days
 */
@Serializable
data class TimeMeasure(
        val time: Long,
        val timeUnit: TimeUnit
) : Comparable<TimeMeasure> {

    companion object {
        /**
         * Regular expression to check if [TimeMeasure] is valid.
         */
        val regex = "^(-?[0-9]+\\.?[0-9]*[eE]?-?[0-9]*)[fFlL]?(ns|us|ms|s|m|h|d)$".toRegex()

        /**
         * Try to parses [TimeMeasure] from its string representation.
         *
         * @param s string representation to try to parse.
         *
         * @return parsed [TimeMeasure] or null if the string can't be parsed.
         */
        fun parseOrNull(s: String): TimeMeasure? {
            if (s.length < 2) return null
            val matches = regex.findAll(s.toLowerCase())
            val (timeS, unitS) = matches.singleOrNull()?.destructured ?: return null
            return TimeMeasure(
                    timeS.toDouble().toLong(),
                    when (unitS.toLowerCase()) {
                        "ns" -> NANOSECONDS
                        "us" -> MICROSECONDS
                        "ms" -> MILLISECONDS
                        "s" -> SECONDS
                        "m" -> MINUTES
                        "h" -> HOURS
                        "d" -> DAYS
                        else -> throw UnsupportedOperationException("Unit `$unitS` is not supported")
                    }
            )
        }

        /**
         * Parses [TimeMeasure] from its string representation.
         *
         * @param s string representation to parse.
         *
         * @return parsed [TimeMeasure].
         * @throws IllegalArgumentException if string can't be parsed.
         */
        fun parse(s: String): TimeMeasure = parseOrNull(s)
                ?: throw IllegalArgumentException("Format invalid, should be: $regex")

    }

    override operator fun compareTo(other: TimeMeasure): Int {
        return asNanoseconds().compareTo(other.asNanoseconds())
    }

    /**
     * Returns the number of nanoseconds in current [TimeMeasure] interval
     */
    fun asNanoseconds(): Long = NANOSECONDS.convert(time, timeUnit)

    /**
     * Adds another [TimeMeasure] to current one. Disregarding of units of both operands, the resulted is in nanoseconds.
     *
     * @param other value to add
     *
     * @return a new instance of [TimeMeasure] in nanoseconds.
     */
    operator fun plus(other: TimeMeasure): TimeMeasure =
            TimeMeasure(asNanoseconds() + other.asNanoseconds(), NANOSECONDS)

    /**
     * Subtracts another [TimeMeasure] from current one. Disregarding of units of both operands, the resulted is in nanoseconds.
     *
     * @param other value to subtract
     *
     * @return a new instance of [TimeMeasure] in nanoseconds.
     */
    operator fun minus(other: TimeMeasure): TimeMeasure =
            TimeMeasure(asNanoseconds() - other.asNanoseconds(), NANOSECONDS)

    /**
     * Parseable string representation of [TimeMeasure]
     */
    override fun toString(): String {
        return "${time}${timeUnit.abbreviation()}"
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is TimeMeasure) return false
        return this.compareTo(other) == 0
    }

    override fun hashCode(): Int {
        var result = time.hashCode()
        result = 31 * result + timeUnit.hashCode()
        return result
    }
}