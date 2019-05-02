package mux.cli.scope

import mux.cli.command.ArgumentMissingException
import mux.cli.command.Command
import mux.cli.command.NewScopeCommand
import mux.cli.Session
import mux.cli.command.GenCommand

class RootScope(private val session: Session) : Scope {

    override fun initialOutput(): String? = null

    override fun prompt(): String = ""

    override fun commands(): Set<Command> {
        return setOf(
                NewScopeCommand(
                        "open",
                        "Opens file for playing and editing. Usage: open <path to file>"
                ) { filename ->
                    val f = (filename
                            ?: throw ArgumentMissingException("You need to specify path to the file to edit"))
                    session.openAudioFile(f)
                    val samples = session.samples()
                    AudioFileScope(f, samples)
                },
                GenCommand()
        )
    }

}

