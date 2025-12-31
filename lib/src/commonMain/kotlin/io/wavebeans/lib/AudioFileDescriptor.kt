package io.wavebeans.lib

enum class BitDepth(val bits: Int, val bytesPerSample: Int) {
    BIT_8(8, 1),
    BIT_16(16, 2),
    BIT_24(24, 3),
    BIT_32(32, 4),
    BIT_64(64, 8);

    companion object {
        fun of(bits: Int): BitDepth = safelyOf(bits) ?: throw UnsupportedOperationException("$bits is unsupported bit depth")
        fun safelyOf(bits: Int): BitDepth? = values().firstOrNull { it.bits == bits }
    }
}

open class AudioFileDescriptor(
        val sampleRate: Float,
        val bitDepth: BitDepth
) {

    open fun copy(
            sampleRate: Float = this.sampleRate,
            bitDepth: BitDepth = this.bitDepth
    ): AudioFileDescriptor = AudioFileDescriptor(sampleRate, bitDepth)


    override fun toString(): String {
        return """
            Generic audio file.
            Sample rate: $sampleRate Hz
            Bit depth: $bitDepth bit
        }}
        """.trimIndent()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as AudioFileDescriptor

        if (sampleRate != other.sampleRate) return false
        if (bitDepth != other.bitDepth) return false

        return true
    }

    override fun hashCode(): Int {
        var result = sampleRate.hashCode()
        result = 31 * result + bitDepth.hashCode()
        return result
    }
}

