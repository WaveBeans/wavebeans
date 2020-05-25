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

@Serializable
data class TimeMeasure(
        val time: Long,
        val timeUnit: TimeUnit
) : Comparable<TimeMeasure> {

    companion object {
        val regex = "^(-?[0-9]+\\.?[0-9]*[eE]?-?[0-9]*)[fFlL]?(ns|us|ms|s|m|h|d)$".toRegex()

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

        fun parse(s: String): TimeMeasure = parseOrNull(s)
                ?: throw IllegalArgumentException("Format invalid, should be: $regex")

    }

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