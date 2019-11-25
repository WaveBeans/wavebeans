package io.wavebeans.cli.command

import io.wavebeans.cli.Session

class OpenFileCommand(session: Session) : AbstractNewSamplesScopeCommand(
        session,
        "open",
        "Opens file for playing and editing. Usage: open <path to file>",
        { filename ->
            TODO()
//
//            val filepath = (filename
//                    ?: throw ArgumentMissingException("You need to specify path to the file to edit"))
//            val f = File(filepath)
//            if (!f.exists()) {
//                throw ArgumentWrongException("`$filepath` is not found.")
//            }
//            val (_, samples) = WavFileReader(FileInputStream(f)).read()
//            Pair(f.nameWithoutExtension, samples)
        })