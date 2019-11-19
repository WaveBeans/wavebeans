package mux.cli.scope

import mux.cli.Session
import mux.cli.command.*

class RootScope(private val session: Session) : Scope {

    override fun initialOutput(): String? = null

    override fun prompt(): String = ""

    @ExperimentalStdlibApi
    override fun commands(): Set<Command> {
        return setOf(
                BenchmarkCommand(session),
                OpenFileCommand(session),
                GenCommand(session),
                SaveFileCommand(session, null, null, null),
                ListStreamsCommand(session)
                //TODO add global selection tool
        )
    }

}

