package io.wavebeans.cli

import io.wavebeans.cli.script.ScriptRunner
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.Option
import org.apache.commons.cli.Options
import java.io.File
import java.io.PrintWriter
import java.util.concurrent.CancellationException
import kotlin.system.measureTimeMillis

/**
 * Class encapsulates all functionality that can be done via CLI interface.
 *
 * All methods are following the same pattern: try*Something*() and are returning [Boolean] if that *something* was done.
 * For interpreter if cli method returned false it should try to call something else or return error.
 *
 * @param cli parse [CommandLine] that was passed to the app
 * @param printer what [PrintWriter] to use for output
 */
class WaveBeansCli(
        private val cli: CommandLine,
        private val printer: PrintWriter = PrintWriter(System.out)
) {

    companion object {
        val name = "wavebeans"
        val f = Option("f", "execute-file", true, "Executes script from file.")
        val e = Option("e", "execute", true, "Executes script from provided string.")
        val time = Option(null, "time", false, "Tracks amount of time the script was running for.")
        val v = Option("v", "verbose", false, "Print some extra execution logging to stdout")
        val h = Option("h", "help", false, "Prints this help")
        val o = Option("o", "overseer", true, "Running script in distributed mode, specify exact overseer. Currently supported only `local`. ")
        val options = Options().of(f, e, time, v, h, o)
    }

    private val verbose = cli.has(v)
    private val trackTime = cli.has(time)

    /**
     * Tries executes script which is provided via string of file. String has priority over the file.
     * Handles `^C` signal properly trying to close streams and flush what is done but is in temporary buffers.
     */
    fun tryScriptExecution(): Boolean {
        return if (cli.has(e) || cli.has(f)) {
            val content = cli.get(e) { it }
                    ?: cli.getRequired(f) { File(it).readText() }

            val overseer = cli.get(o) { it }

            val executionTime = measureTimeMillis {
                ScriptRunner(content, closeTimeout = Long.MAX_VALUE, overseer = overseer).start()
                        .also { if (verbose) printer.println("Script started:\n$content") }
                        .use { runner ->
                            Runtime.getRuntime().addShutdownHook(Thread {
                                if (verbose) printer.println("Shutting down the script...")
                                runner.interrupt(waitToFinish = true)
                                runner.close()
                                if (verbose) printer.println("Shut down finished.")
                            })

                            try {
                                runner.awaitForResult()
                                        .also {
                                            if (verbose)
                                                printer.println("Finished with result: " + (it?.message ?: "Success"))
                                        }
                            } catch (e: CancellationException) {
                                if (verbose) printer.println("Script execution cancelled")
                            }

                            Unit
                        }
            }
            if (trackTime) printer.println("${executionTime / 1000.0}sec")
            printer.flush()
            true
        } else {
            false
        }
    }

}