package mux.cli.command

import mux.lib.stream.SampleStream
import java.lang.Math.max
import java.lang.Math.min
import javax.sound.sampled.AudioSystem


class PlayCommand(
        val samples: SampleStream,
        val start: Int?,
        val end: Int?
) : InScopeCommand("play", "Play the whole file from the beginning or selection if any.", { _, _ ->

    val clip = AudioSystem.getClip()!!
//    val data = samples.toByteArray()
//    val s = max(start?.let { it * samples.descriptor.bitDepth / 8 } ?: 0, 0)
//    val e = min(end?.let { it * samples.descriptor.bitDepth / 8 } ?: Int.MAX_VALUE, data.size)
//    clip.open(
//            samples.descriptor.toAudioFormat(),
//            data,
//            s,
//            e - s
//    )
//    clip.start()
    TODO()

    "Playing $samples from $start to $end"
})