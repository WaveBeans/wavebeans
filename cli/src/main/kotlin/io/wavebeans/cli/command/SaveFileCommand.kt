package io.wavebeans.cli.command

import io.wavebeans.cli.Session

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
        { _, _ ->

//            val outputBitDepth = session.outputDescriptor.bitDepth
//            val outputSampleRate = session.outputDescriptor.sampleRate
//
//            val file = File(args ?: throw ArgumentMissingException("You should specify path to file"))
//            file.createNewFile()
//
//            val samples = streamName?.let { session.streamByName(it) } ?: TODO("merge all streams")
//
//            val sampleStream = if (start != null || end != null) {
////                samples.rangeProjection(start ?: 0, end ?: samples.samplesCount() - 1)
//                TODO()
//            } else {
//                samples
//            }

//            val writer = WavFileOutput(WavLEAudioFileDescriptor(
//                    outputSampleRate,
//                    outputBitDepth,
//                    1
//            ), FileOutputStream(file))
//            writer.write(sampleStream)
//            "Saved [$start, $end] to `$file`"
            TODO()
        })