package mux.cli.command

import mux.lib.AudioFileDescriptor

data class Selection(val start: SelectionValue, val end: SelectionValue) {
    companion object {
        fun parse(input: String): Selection {
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
                                SampleSelectionValue(v.toInt())
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
    fun sampleIndex(sampleRate: Float): Int
}

/** Selection base on sample index. */
data class SampleSelectionValue(val value: Int) : SelectionValue {
    override fun sampleIndex(sampleRate: Float): Int = value

    override fun toString(): String {
        return "$value"
    }
}

/** Time based selection. Value is in milliseconds. */
data class TimeSelectionValue(val value: Long) : SelectionValue {
    override fun sampleIndex(sampleRate: Float): Int {
        val samplesPerMs = sampleRate / 1000.0f
        return (value * samplesPerMs).toInt()
    }

    override fun toString(): String {
        val ms = value % 1000
        val s = value / 1000
        return "$s.${ms.toString().padStart(3, '0')}s"
    }

}

class SelectionParseException(message: String) : Exception(message)