package io.wavebeans.lib.io

import io.wavebeans.lib.AudioFileDescriptor
import io.wavebeans.lib.BitDepth
import javax.sound.sampled.AudioFormat

/** RIFF constant which should always be first 4 bytes of the wav-file. */
const val RIFF = 0x52494646
/** WAVE constant. */
const val WAVE = 0x57415645
/** "fmt " constant */
const val FMT = 0x666D7420
/** PCM format constant */
const val PCM_FORMAT = 0x0100.toShort()
/** "data" constant */
const val DATA = 0x64617461

class WavLEAudioFileDescriptor(
        sampleRate: Float,
        bitDepth: BitDepth,
        val numberOfChannels: Int,
        val dataSize: Int,
) : AudioFileDescriptor(
        sampleRate,
        bitDepth
) {

    private val frameSize: Int = numberOfChannels * bitDepth.bytesPerSample
    private val frameRate: Int = sampleRate.toInt() / numberOfChannels
    private val bigEndian: Boolean = false

    override fun copy(sampleRate: Float, bitDepth: BitDepth): AudioFileDescriptor =
            WavLEAudioFileDescriptor(sampleRate, bitDepth, numberOfChannels, dataSize)

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