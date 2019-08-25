package mux.lib

import javax.sound.sampled.AudioFormat

enum class BitDepth(val bits: Int, val bytesPerSample: Int) {
    BIT_8(8, 1),
    BIT_16(16, 2),
    BIT_24(24, 3),
    BIT_32(32, 4),
    BIT_64(64, 8);

    companion object {
        fun of(bits: Int): BitDepth = values().firstOrNull { it.bits == bits } ?: throw UnsupportedOperationException("$bits is unsupported bit depth")
    }
}

open class AudioFileDescriptor(
        val sampleRate: Float,
        val bitDepth: BitDepth
) {
    open fun toAudioFormat(): AudioFormat = throw UnsupportedOperationException()

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
        if (javaClass != other?.javaClass) return false

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

class WavLEAudioFileDescriptor(
        sampleRate: Float,
        bitDepth: BitDepth,
        val numberOfChannels: Int
) : AudioFileDescriptor(
        sampleRate,
        bitDepth
) {

    private val frameSize: Int = numberOfChannels * bitDepth.bytesPerSample
    private val frameRate: Int = sampleRate.toInt() / numberOfChannels
    private val bigEndian: Boolean = false

    override fun copy(sampleRate: Float, bitDepth: BitDepth): AudioFileDescriptor =
            WavLEAudioFileDescriptor(sampleRate, bitDepth, numberOfChannels)

    override fun toAudioFormat(): AudioFormat {
        return AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                sampleRate,
                bitDepth.bits,
                numberOfChannels,
                frameSize,
                frameRate.toFloat(),
                bigEndian
        )
    }


    override fun toString(): String {
        return """
            PCM Wave file.
            Sample rate: $sampleRate.0 Hz
            Bit depth: $bitDepth bit
            Channels: ${when (numberOfChannels) {
            1 -> "1 (Mono)"
            2 -> "2 (Stereo)"
            else -> "$numberOfChannels"
        }}
        """.trimIndent()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as WavLEAudioFileDescriptor

        if (numberOfChannels != other.numberOfChannels) return false
        if (frameSize != other.frameSize) return false
        if (frameRate != other.frameRate) return false
        if (bigEndian != other.bigEndian) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + numberOfChannels
        result = 31 * result + frameSize
        result = 31 * result + frameRate
        result = 31 * result + bigEndian.hashCode()
        return result
    }

}