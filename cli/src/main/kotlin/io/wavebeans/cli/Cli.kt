package io.wavebeans.cli

import io.wavebeans.cli.WaveBeansCli.Companion.h
import io.wavebeans.cli.WaveBeansCli.Companion.name
import io.wavebeans.cli.WaveBeansCli.Companion.options
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.MissingOptionException
import java.io.PrintWriter
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val cli = try {
        DefaultParser().parse(options, args)
    } catch (e: MissingOptionException) {
        println("ERROR: ${e.message}")
        printHelp()
        exitProcess(1)
    }

    if (cli.has(h) || args.isEmpty()) {
        printHelp()
        exitProcess(1)
    }

    val waveBeansCli = WaveBeansCli(cli)

    if (waveBeansCli.tryScriptExecution()) {
        exitProcess(0)
    } else {
        println("ERROR: No clear instructions provided.")
        printHelp()
        exitProcess(1)
    }
}

private fun printHelp() {
    val formatter = HelpFormatter()
    val writer = PrintWriter(System.out)
    formatter.printUsage(writer, 80, name, options)
    writer.println()
    formatter.printOptions(writer, 80, options, 0, 0)
    writer.flush()
}
