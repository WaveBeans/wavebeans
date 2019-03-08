package mux.cli.scope

import mux.cli.command.ArgumentMissingException
import mux.cli.command.Command
import mux.cli.command.NewScopeCommand
import mux.cli.Session

class RootScope(val session: Session) : Scope {

    override fun initialOutput(): String? = null

    override fun prompt(): String = ""

    override fun commands(): Set<Command> {
        return setOf(
                NewScopeCommand(
                        "open",
                        "Opens file for playing and editing. Usage: open <path to file>"
                ) { filename ->
                    AudioFileScope(session, filename
                            ?: throw ArgumentMissingException("You need to specify path to the file to edit"))
                }
        )
    }

}

