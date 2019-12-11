package io.wavebeans.cli

import io.wavebeans.cli.script.ScriptRunner
import org.apache.commons.cli.*
import java.io.File
import java.io.PrintWriter
import java.util.concurrent.CancellationException
import kotlin.system.exitProcess

private val f = Option("f", "file", true, "Executes script from file.")
private val e = Option("e", "execute", true, "Executes script from provided string.")
private val time = Option(null, "time", false, "Tracks amount of time the script was running for.")
private val v = Option("v", "verbose", false, "Print some extra execution logging to stdout")
private val h = Option("h", "help", false, "Prints this help")
private val options = Options().of(f, e, time, v, h)


fun main(args: Array<String>) {

    val cli = try {
        DefaultParser().parse(options, args)
    } catch (e: MissingOptionException) {
        println("ERROR: ${e.message}")
        printHelp()
        exitProcess(1)
    }

    val verbose = cli.has(v)

    if (cli.has(h) || args.isEmpty()) {
        printHelp()
        exitProcess(1)
    }

    if (tryScriptExecution(cli, verbose)) {
        exitProcess(0)
    } else {
        println("ERROR: No clear instructions provided.")
        printHelp()
        exitProcess(1)
    }
}

private fun tryScriptExecution(cli: CommandLine, verbose: Boolean): Boolean {
    return if (cli.has(e) || cli.has(f)) {
        val content = cli.get(e) { it }
                ?: cli.getRequired(f) { File(it).readText() }

        ScriptRunner(content, closeTimeout = Long.MAX_VALUE).start()
                .also { if (verbose) println("Script started:\n$content") }
                .use { runner ->
                    Runtime.getRuntime().addShutdownHook(Thread {
                        if (verbose) println("Shutting down the script...")
                        runner.interrupt(waitToFinish = true)
                        runner.close()
                        if (verbose) println("Shut down finished.")
                    })

                    try {
                        runner.awaitForResult()
                                .also { if (verbose) println("Finished with result: " + (it?.message ?: "Success")) }
                    } catch (e: CancellationException) {
                        if (verbose) println("Script execution cancelled")
                    }

                    Unit
                }
        true
    } else {
        false
    }
}

private fun printHelp() {
    val formatter = HelpFormatter()
    val writer = PrintWriter(System.out)
    formatter.printUsage(writer, 80, "tokens-cli", options)
    writer.println()
    formatter.printOptions(writer, 80, options, 0, 0)
    writer.flush()
}
