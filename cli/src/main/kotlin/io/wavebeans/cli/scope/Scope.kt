package io.wavebeans.cli.scope

import io.wavebeans.cli.command.Command
import io.wavebeans.cli.command.CommandException

interface Scope {
    fun prompt(): String

    fun commands(): Set<Command>

    fun initialOutput(): String?

    fun commandByName(name: String): Command? = commands().firstOrNull { it.name().toLowerCase() == name }
}

class ScopeException(message: String) : CommandException(message)