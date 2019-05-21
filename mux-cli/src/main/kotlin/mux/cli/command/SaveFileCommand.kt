package mux.cli.command

import mux.cli.Session
import mux.lib.BitDepth
import mux.lib.WavLEAudioFileDescriptor
import mux.lib.file.WavFileWriter
import mux.lib.stream.SampleStream
import java.io.File
import java.io.FileOutputStream

class SaveFileCommand(
        val session: Session,
        val streamName: String?,
        val start: Int?,
        val end: Int?
) : InScopeCommand("save", """
            Saves samples into a wav file.
            Saves selection or the whole file depening on where it is invoked. The format of file is defined in current descriptor.
            Usage: save <path to file>
            """
        .trimIndent(),
        { _, args ->

            val file = File(args ?: throw ArgumentMissingException("You should specify path to file"))
            file.createNewFile()

            val samples = session.streamByName(streamName) ?: TODO("merge all streams")

            val sampleStream = if (start != null || end != null) {
                samples.rangeProjection(start ?: 0, end ?: samples.samplesCount() - 1)
            } else {
                samples
            }

            val writer = WavFileWriter(WavLEAudioFileDescriptor(
                    sampleStream.sampleRate,
                    BitDepth.BIT_16,
                    1
            ), FileOutputStream(file))
            writer.write(sampleStream)
            "Saved [$start, $end] to `$file`"
        })