package mux.cli.command

import mux.lib.stream.SampleStream
import java.io.File
import java.io.FileOutputStream

class SaveFileCommand(
        val samples: SampleStream,
        val start: Int?,
        val end: Int?
) : InScopeCommand("save", """
            Saves samples into a file.
            Saves selection or the whole file depening on where it is invoked. The format of file is defined in current descriptor.
            Usage: save <path to file>
            """
        .trimIndent(),
        { _, args ->

            val file = File(args ?: throw ArgumentMissingException("You should specify path to file"))
            file.createNewFile()

            val sampleStream = if (start != null || end != null) {
                samples.rangeProjection(start ?: 0, end ?: samples.samplesCount() - 1)
            } else {
                samples
            }

//            val writer = samples.descriptor.getWriter(FileOutputStream(file))
//            writer.write(sampleStream)
            TODO()
            "Saved [$start, $end] to `$file`"
        })