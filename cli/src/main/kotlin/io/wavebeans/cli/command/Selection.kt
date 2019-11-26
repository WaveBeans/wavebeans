package io.wavebeans.cli.command

import io.wavebeans.lib.samplesCountToLength
import java.util.concurrent.TimeUnit

data class Selection(val start: SelectionValue, val end: SelectionValue) {
    companion object {
        fun parse(sampleRate: Float, input: String): Selection {
            val (start, end) = input.trim()
                    .split("..", limit = 2)
                    .map { it.trim() }
                    .map { v ->
                        when {
                            v.matches(Regex("^\\d+\\.\\d{3}\\s*s$")) -> {
                                TimeSelectionValue(v.removeSuffix("s").trim()
                                        .replace(".", "")
                                        .toLong())
                            }
                            v.matches(Regex("^\\d+\\s*ms$")) -> {
                                TimeSelectionValue(v.removeSuffix("ms").trim()
                                        .toLong())
                            }
                            v.matches(Regex("^\\d+\\s*s$")) -> {
                                TimeSelectionValue(v.removeSuffix("s").trim()
                                        .toLong() * 1000)
                            }
                            v.matches(Regex("^\\d+$")) -> {
                                SampleSelectionValue(sampleRate, v.toLong())
                            }
                            else -> throw SelectionParseException("`$v` is unsupported format")
                        }
                    }
            return Selection(start, end)
        }
    }

    override fun toString(): String {
        return "$start..$end"
    }
}

interface SelectionValue {
    fun time(timeUnit: TimeUnit): Long
}

/** Selection base on sample index. */
data class SampleSelectionValue(val sampleRate: Float, val value: Long) : SelectionValue {

    override fun time(timeUnit: TimeUnit): Long = samplesCountToLength(value, sampleRate, timeUnit)

    override fun toString(): String {
        return "$value"
    }
}

/** Time based selection. Value is in milliseconds. */
data class TimeSelectionValue(val value: Long) : SelectionValue {

    override fun time(timeUnit: TimeUnit): Long = timeUnit.convert(value, TimeUnit.MILLISECONDS)

    override fun toString(): String {
        val ms = value % 1000
        val s = value / 1000
        return "$s.${ms.toString().padStart(3, '0')}s"
    }

}

class SelectionParseException(message: String) : Exception(message)