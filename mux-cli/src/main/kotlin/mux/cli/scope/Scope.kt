package mux.cli.scope

import mux.cli.command.Command
import mux.cli.command.CommandException

interface Scope {
    fun prompt(): String

    fun commands(): Set<Command>

    fun initialOutput(): String?

    fun commandByName(name: String): Command? = commands().firstOrNull { it.name().toLowerCase() == name }
}

class ScopeException(message: String) : CommandException(message)