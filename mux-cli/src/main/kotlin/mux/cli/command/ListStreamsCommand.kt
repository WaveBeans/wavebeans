package mux.cli.command

import mux.cli.Session

class ListStreamsCommand(val session: Session) : InScopeCommand(
        "list",
                "Lists all available stream",
        { _, _ ->
                TODO()
        }
)