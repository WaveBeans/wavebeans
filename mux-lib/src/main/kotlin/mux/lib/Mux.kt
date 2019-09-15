package mux.lib

data class Mux(
        val desc: String,
        val stream: MuxStream<*, *>?
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Mux

        if (desc != other.desc) return false
        if (stream != other.stream) return false

        return true
    }

    override fun hashCode(): Int {
        var result = desc.hashCode()
        result = 31 * result + (stream?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "Mux(desc='$desc')"
    }
}