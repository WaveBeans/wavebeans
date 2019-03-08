package mux.lib

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

    override fun toString(): String {
        return "${this.javaClass.simpleName}(sampleRate=$sampleRate, bitDepth=$bitDepth, numberOfChannels=$numberOfChannels, frameSize=$frameSize, frameRate=$frameRate, bigEndian=$bigEndian)"
    }


}

class WavLEAudioFileDescriptor(
        sampleRate: Int,
        bitDepth: Int,
        numberOfChannels: Int,
        val dataSize: Int
) : AudioFileDescriptor(sampleRate, bitDepth, numberOfChannels, numberOfChannels * bitDepth / 8, sampleRate / numberOfChannels, false) {
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
            Size: $dataSize bytes
            Length: ${dataSize / (bitDepth / 8) / sampleRate.toFloat()} sec
        """.trimIndent()
    }


}