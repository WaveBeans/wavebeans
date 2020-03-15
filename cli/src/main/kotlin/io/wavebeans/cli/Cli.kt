package io.wavebeans.cli

import io.wavebeans.cli.WaveBeansCli.Companion.h
import io.wavebeans.cli.WaveBeansCli.Companion.name
import io.wavebeans.cli.WaveBeansCli.Companion.options
import io.wavebeans.cli.WaveBeansCli.Companion.v
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.MissingOptionException
import java.io.PrintWriter
import java.util.jar.JarFile
import java.util.jar.Manifest
import kotlin.system.exitProcess

@ExperimentalStdlibApi
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

    if (cli.has(v)) {
        printVersion()
        exitProcess(0)
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
    formatter.printWrapped(writer, 80, "WaveBeans v.${readVersion()}\n\n")
    formatter.printUsage(writer, 80, name, options)
    writer.println()
    formatter.printOptions(writer, 80, options, 0, 0)
    writer.flush()
}

private fun printVersion() {
    val formatter = HelpFormatter()
    val writer = PrintWriter(System.out)
    formatter.printWrapped(writer, 80, "${readVersion()}")
    writer.flush()
}

private fun readVersion(): String {
    return Thread.currentThread().contextClassLoader.getResources(JarFile.MANIFEST_NAME)
            .asSequence()
            .mapNotNull {
                it.openStream().use { stream ->
                    val attributes = Manifest(stream).mainAttributes
                    attributes.getValue("WaveBeans-Version")
                }
            }
            .firstOrNull()
            ?: "<NOT VERSIONED>"
}