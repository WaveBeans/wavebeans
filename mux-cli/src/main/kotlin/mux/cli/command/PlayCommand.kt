package mux.cli.command

import mux.cli.Session
import mux.lib.WavLEAudioFileDescriptor
import mux.lib.io.ByteArrayLittleEndianFileOutput
import mux.lib.stream.SampleStream
import javax.sound.sampled.AudioSystem


class PlayCommand(
        val session: Session,
        val samples: SampleStream,
        val start: Int?,
        val end: Int?
) : InScopeCommand("play", "Play the whole file from the beginning or selection if any.", { _, _ ->

//    val outputBitDepth = session.outputDescriptor.bitDepth
//    val outputSampleRate = session.outputDescriptor.sampleRate
//
//    val descriptor = WavLEAudioFileDescriptor(outputSampleRate, outputBitDepth, 1)
//    val output = ByteArrayLittleEndianFileOutput(outputSampleRate, outputBitDepth, samples)
//
//    val data = output.toByteArray()
//    val s = start?.let { it * outputBitDepth.bytesPerSample } ?: 0
//    val e = end?.let { it * outputBitDepth.bytesPerSample } ?: data.size
//
//    val clip = AudioSystem.getClip()!!
//    clip.open(
//            descriptor.toAudioFormat(),
//            data,
//            s,
//            e - s
//    )
//    clip.start()
//
//    "Playing $samples from $start to $end"
    TODO()
})