package mux.cli.scope

import mux.cli.Session
import mux.cli.command.*

class RootScope(private val session: Session) : Scope {

    override fun initialOutput(): String? = null

    override fun prompt(): String = ""

    override fun commands(): Set<Command> {
        return setOf(
                OpenFileCommand(session),
                GenCommand(session),
                SaveFileCommand(session, null, null, null)
                //TODO add global selection tool
        )
    }

}

