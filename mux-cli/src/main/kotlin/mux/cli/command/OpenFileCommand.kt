package mux.cli.command

import mux.cli.Session
import mux.lib.file.WavFileReader
import java.io.File
import java.io.FileInputStream

class OpenFileCommand(session: Session) : AbstractNewSamplesScopeCommand(
        session,
        "open",
        "Opens file for playing and editing. Usage: open <path to file>",
        { filename ->


            val filepath = (filename
                    ?: throw ArgumentMissingException("You need to specify path to the file to edit"))
            val f = File(filepath)
            if (!f.exists()) {
                throw ArgumentWrongException("`$filepath` is not found.")
            }
            val (_, samples) = WavFileReader(FileInputStream(f)).read()
            Pair(f.nameWithoutExtension, samples)
        })