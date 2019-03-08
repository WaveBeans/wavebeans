package mux.cli.command

import mux.cli.scope.Scope

interface Command {
    fun name(): String

    fun description(): String
}

open class NewScopeCommand(
        val name: String,
        val description: String,
        val scopeCreator: (String?) -> Scope
) : Command {

    override fun name(): String = name

    override fun description(): String = description

    fun newScope(args: String?) = scopeCreator(args)
}

open class InScopeCommand(
        val name: String,
        val description: String,
        val scopeModifier: (Scope, String?) -> String?
) : Command {

    override fun name(): String = name

    override fun description(): String = description

    fun run(caller: Scope, args: String?): String? = scopeModifier(caller, args)
}

abstract class CommandException(message: String) : Exception(message)

class ArgumentMissingException(message: String) : CommandException(message)

class ArgumentWrongException(message: String) : CommandException(message)
