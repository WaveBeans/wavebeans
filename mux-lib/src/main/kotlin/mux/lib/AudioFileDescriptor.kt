package mux.lib

import java.io.OutputStream
import javax.sound.sampled.AudioFormat

abstract class AudioFileDescriptor(
        val sampleRate: Int,
        val bitDepth: Int,
        val numberOfChannels: Int,
        val frameSize: Int,
        val frameRate: Int,
        val bigEndian: Boolean
) {
    abstract fun toAudioFormat(): AudioFormat

    abstract fun getWriter(destination: OutputStream): AudioFileWriter<AudioFileDescriptor>

    override fun toString(): String {
        return "${this.javaClass.simpleName}(sampleRate=$sampleRate, bitDepth=$bitDepth, numberOfChannels=$numberOfChannels, frameSize=$frameSize, frameRate=$frameRate, bigEndian=$bigEndian)"
    }

    abstract fun copy(
            sampleRate: Int = this.sampleRate,
            bitDepth: Int = this.bitDepth,
            numberOfChannels: Int = this.numberOfChannels,
            frameSize: Int = this.frameSize,
            frameRate: Int = this.frameRate,
            bigEndian: Boolean = this.bigEndian
    ): AudioFileDescriptor

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AudioFileDescriptor

        if (sampleRate != other.sampleRate) return false
        if (bitDepth != other.bitDepth) return false
        if (numberOfChannels != other.numberOfChannels) return false
        if (frameSize != other.frameSize) return false
        if (frameRate != other.frameRate) return false
        if (bigEndian != other.bigEndian) return false

        return true
    }

    override fun hashCode(): Int {
        var result = sampleRate
        result = 31 * result + bitDepth
        result = 31 * result + numberOfChannels
        result = 31 * result + frameSize
        result = 31 * result + frameRate
        result = 31 * result + bigEndian.hashCode()
        return result
    }


}

class WavLEAudioFileDescriptor(
        sampleRate: Int,
        bitDepth: Int,
        numberOfChannels: Int
) : AudioFileDescriptor(
        sampleRate,
        bitDepth,
        numberOfChannels,
        numberOfChannels * bitDepth / 8,
        sampleRate / numberOfChannels,
        false
) {
    override fun copy(sampleRate: Int, bitDepth: Int, numberOfChannels: Int, frameSize: Int, frameRate: Int, bigEndian: Boolean): AudioFileDescriptor =
            WavLEAudioFileDescriptor(sampleRate, bitDepth, numberOfChannels)

    override fun getWriter(destination: OutputStream): AudioFileWriter<AudioFileDescriptor> {
        return WavFileWriter(this, destination)
    }

    override fun toAudioFormat(): AudioFormat {
        return AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                sampleRate.toFloat(),
                bitDepth,
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

}