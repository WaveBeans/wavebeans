package mux.cli

import mux.cli.command.ArgumentMissingException
import mux.cli.command.ArgumentWrongException
import mux.cli.command.InScopeCommand
import mux.cli.command.NewScopeCommand
import mux.cli.scope.RootScope
import mux.cli.scope.Scope
import mux.cli.scope.ScopeException
import org.jline.reader.LineReaderBuilder
import org.jline.terminal.TerminalBuilder
import java.lang.System.exit

fun main() {
    val terminal = TerminalBuilder
            .builder()
            .dumb(true)
            .build()!!
    val lineReader = LineReaderBuilder.builder()
            .terminal(terminal)
            .build()!!

    val scopes = ArrayList<Scope>()
    scopes += RootScope(Session())
    while (true) {
        val currentScope = scopes.last()
        val readLine = lineReader.readLine(currentScope.prompt() + "> ") ?: ""
        val input = readLine.trim()
        if (input.trim().isEmpty()) continue
        val cmdAndArgs = input.split(" ", limit = 2)
        val cmdName = cmdAndArgs[0]
        val args = if (cmdAndArgs.size > 1) cmdAndArgs[1] else null
        when {
            cmdName == "?" -> {
                currentScope.commands().forEach {
                    println("${it.name()} -- ${it.description()}")
                }
            }
            cmdName.toLowerCase() == "exit" -> {
                println("Bye!")
                exit(0)
            }
            cmdName.toLowerCase() == "fin" -> scopes.removeAt(scopes.size - 1)
            else -> {
                val command = currentScope.commandByName(cmdName)
                when (command) {
                    null -> println("Command is not recognized: $cmdName")
                    is NewScopeCommand -> {
                        try {
                            val newScope = command.newScope(args)
                            val output = newScope.initialOutput()
                            if (output != null) println(output)
                            scopes.add(newScope)
                        } catch (e: ArgumentMissingException) {
                            println("${command.name}: ${e.message}")
                        } catch (e: ArgumentWrongException) {
                            println("${command.name}: ${e.message}")
                        }
                    }
                    is InScopeCommand -> {
                        val output = command.run(currentScope, args)
                        if (output != null) println(output)
                    }
                    else -> throw UnsupportedOperationException("$command is unsupported")
                }
            }
        }
    }
}
