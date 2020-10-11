package io.wavebeans.tests

data class CommandResult(
        val exitCode: Int,
        val output: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CommandResult

        if (exitCode != other.exitCode) return false
        if (!output.contentEquals(other.output)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = exitCode
        result = 31 * result + output.contentHashCode()
        return result
    }
}