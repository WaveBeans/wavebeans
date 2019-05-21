package mux.cli.command

import mux.lib.BitDepth
import mux.lib.WavLEAudioFileDescriptor
import mux.lib.io.ByteArrayLittleEndianAudioOutput
import mux.lib.stream.SampleStream
import java.lang.Math.max
import java.lang.Math.min
import javax.sound.sampled.AudioSystem


class PlayCommand(
        val samples: SampleStream,
        val start: Int?,
        val end: Int?
) : InScopeCommand("play", "Play the whole file from the beginning or selection if any.", { _, _ ->

    val outputBitDepth = BitDepth.BIT_16

    val descriptor = WavLEAudioFileDescriptor(samples.sampleRate, outputBitDepth, 1)
    val output = ByteArrayLittleEndianAudioOutput(outputBitDepth, samples)

    val data = output.toByteArray()
    val s = start?.let { it * outputBitDepth.bytesPerSample } ?: 0
    val e = end?.let { it * outputBitDepth.bytesPerSample } ?: data.size

    val clip = AudioSystem.getClip()!!
    clip.open(
            descriptor.toAudioFormat(),
            data,
            s,
            e - s
    )
    clip.start()

    "Playing $samples from $start to $end"
})