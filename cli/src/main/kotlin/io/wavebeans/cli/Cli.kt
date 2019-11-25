package io.wavebeans.cli

import io.wavebeans.cli.command.ArgumentMissingException
import io.wavebeans.cli.command.ArgumentWrongException
import io.wavebeans.cli.command.InScopeCommand
import io.wavebeans.cli.command.NewScopeCommand
import io.wavebeans.cli.scope.RootScope
import io.wavebeans.cli.scope.Scope
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.terminal.TerminalBuilder
import java.io.OutputStream
import java.io.PrintStream

fun main() {
    val terminal = TerminalBuilder
            .builder()
            .dumb(true)
            .build()!!
    val lineReader = LineReaderBuilder.builder()
            .terminal(terminal)
            .build()!!

    Cli(lineReader, System.out).run()
}

class Cli(private val lineReader: LineReader, outputStream: OutputStream) : Runnable {

    private val out = PrintStream(outputStream)

    override fun run() {
        val scopes = ArrayList<Scope>()
        scopes += RootScope(Session())

        while (scopes.isNotEmpty()) {
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
                        out.println("${it.name()} -- ${it.description()}")
                    }
                }
                cmdName.toLowerCase() == "exit" -> {
                    out.println("Bye!")
                    scopes.clear()
                }
                cmdName.toLowerCase() == "fin" -> scopes.removeAt(scopes.size - 1)
                else -> {
                    val command = currentScope.commandByName(cmdName)
                    try {
                        when (command) {
                            null -> out.println("Command is not recognized: $cmdName")
                            is NewScopeCommand -> {
                                val newScope = command.newScope(args)
                                val output = newScope.initialOutput()
                                if (output != null) println(output)
                                scopes.add(newScope)
                            }
                            is InScopeCommand -> {
                                val output = command.run(currentScope, args)
                                if (output != null) out.println(output)
                            }
                            else -> throw UnsupportedOperationException("$command is unsupported")
                        }
                    } catch (e: ArgumentMissingException) {
                        out.println("${command?.name()}: ${e.message}")
                    } catch (e: ArgumentWrongException) {
                        out.println("${command?.name()}: ${e.message}")
                    }
                }
            }
            out.flush()
        }

    }
}