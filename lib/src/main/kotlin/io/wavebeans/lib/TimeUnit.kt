package io.wavebeans.lib

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

val Number.ns
    get() = TimeMeasure(this.toLong(), NANOSECONDS)

val Number.us
    get() = TimeMeasure(this.toLong(), MICROSECONDS)

val Number.ms
    get() = TimeMeasure(this.toLong(), MILLISECONDS)

val Number.s
    get() = TimeMeasure(this.toLong(), SECONDS)

val Number.m
    get() = TimeMeasure(this.toLong(), MINUTES)

val Number.h
    get() = TimeMeasure(this.toLong(), HOURS)

val Number.d
    get() = TimeMeasure(this.toLong(), DAYS)

data class TimeMeasure(
        val time: Long,
        val timeUnit: TimeUnit
) : Comparable<TimeMeasure> {

    override operator fun compareTo(other: TimeMeasure): Int {
        return asNanoseconds().compareTo(other.asNanoseconds())
    }

    fun asNanoseconds(): Long = NANOSECONDS.convert(time, timeUnit)

    operator fun plus(other: TimeMeasure): TimeMeasure =
            TimeMeasure(asNanoseconds() + other.asNanoseconds(), NANOSECONDS)

    operator fun minus(other: TimeMeasure): TimeMeasure =
            TimeMeasure(asNanoseconds() - other.asNanoseconds(), NANOSECONDS)

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