package io.wavebeans.cli

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import io.wavebeans.cli.script.RunMode
import io.wavebeans.cli.script.ScriptRunner
import io.wavebeans.http.HttpService
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.Option
import org.apache.commons.cli.Options
import org.slf4j.LoggerFactory
import java.io.File
import java.io.PrintWriter
import java.lang.Thread.sleep
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
        val verbose = Option(null, "verbose", false, "Print some extra execution logging to stdout")
        val h = Option("h", "help", false, "Prints this help")
        val m = Option("m", "run-mode", true, "Running script in distributed mode, specify exact overseer. Default is: ${RunMode.LOCAL.id}. Supported: ${RunMode.values().joinToString(", ") { it.id }}. ")
        val p = Option("p", "partitions", true, "Number of partitions to use in Distributed mode.")
        val t = Option("t", "threads", true, "Number of threads to use in Distributed mode.")
        val s = Option("s", "sample-rate", true, "Sample rate in Hz to use for outputs. By default, it's 44100.")
        val v = Option("v", "version", false, "Prints version of the tool.")
        val debug = Option(null, "debug", false, "DEBUG level of logging in to file under `logs` directory. By default it is INFO")
        val http = Option(null, "http", true, "Run HTTP API on the specified port.")
        val httpWait = Option(null, "http-wait", true, "Time to wait after script finish to shutdown the server in seconds, by default it doesn't wait (0 value), -1 to wait indefinitely")
        val options = Options().of(f, e, time, verbose, h, m, p, t, s, debug, v, http, httpWait)
    }

    private val verbosePrint = cli.has(verbose)
    private val trackTime = cli.has(time)

    /**
     * Tries executes script which is provided via string of file. String has priority over the file.
     * Handles `^C` signal properly trying to close streams and flush what is done but is in temporary buffers.
     */
    @ExperimentalStdlibApi
    fun tryScriptExecution(): Boolean {

        if (cli.has(debug)) {
            val lc = LoggerFactory.getILoggerFactory() as LoggerContext
            val logbackLogger = lc.getLogger("ROOT")
            logbackLogger.level = Level.DEBUG
        }

        return if (cli.has(e) || cli.has(f)) {
            val content = cli.get(e) { it }
                    ?: cli.getRequired(f) { File(it).readText() }

            val runMode = cli.get(m) { RunMode.byId(it) } ?: RunMode.LOCAL
            if (verbosePrint) printer.printLine("Running mode: ${runMode.id}")
            val runOptions = mutableMapOf<String, Any>()
            if (runMode == RunMode.LOCAL_DISTRIBUTED && cli.has(p)) runOptions["partitions"] = cli.getRequired(p) { it.toInt() }
            if (runMode == RunMode.LOCAL_DISTRIBUTED && cli.has(t)) runOptions["threads"] = cli.getRequired(t) { it.toInt() }
            val sampleRate = cli.get(s) { it.toFloat() } ?: 44100.0f

            val httpWait = cli.get(httpWait) { it.toLong() } ?: 0
            val httpService = cli.get(http) { HttpService(serverPort = it.toInt()) }?.start()
            var cancellingAheadOfTime = false

            try {
                val executionTime = measureTimeMillis {
                    ScriptRunner(
                            content,
                            closeTimeout = Long.MAX_VALUE,
                            runMode = runMode,
                            runOptions = runOptions,
                            sampleRate = sampleRate
                    ).use { runner ->
                        try {
                            runner.start()
                            if (verbosePrint) printer.printLine("Script started:\n$content")

                            Runtime.getRuntime().addShutdownHook(Thread {
                                if (verbosePrint) printer.printLine("Shutting down the script...")
                                runner.interrupt(waitToFinish = true)
                                runner.close()
                                if (verbosePrint) printer.printLine("Shut down finished.")
                            })

                            try {
                                runner.awaitForResult()
                                        .also {
                                            if (verbosePrint)
                                                printer.printLine("Finished with result: " + (it?.message
                                                        ?: "Success"))
                                        }
                            } catch (e: CancellationException) {
                                if (verbosePrint) printer.printLine("Script execution cancelled")
                                cancellingAheadOfTime = true
                            }
                        } catch (e: Exception) {
                            e.message?.let { printer.printLine(it) } ?: e.printStackTrace(printer)
                        }

                        Unit
                    }
                }
                if (trackTime) printer.printLine("${executionTime / 1000.0}sec")
            } finally {
                if (httpService != null) {
                    if (httpWait != 0L && !cancellingAheadOfTime) {
                        val endTime = if (httpWait > 0) System.currentTimeMillis() + httpWait * 1000L else Long.MAX_VALUE
                        if (verbosePrint) printer.printLine("HTTP API keep running")
                        try {
                            while (System.currentTimeMillis() < endTime) {
                                sleep(1)
                            }
                        } catch (e: InterruptedException) {
                            if (verbosePrint) printer.printLine("Closing HTTP API")
                            httpService.close()
                        }
                    } else {
                        if (verbosePrint) printer.printLine("Closing HTTP API")
                        httpService.close()
                    }
                }
            }
            true
        } else {
            false
        }
    }

}

private fun PrintWriter.printLine(m: String): PrintWriter {
    this.println(m)
    this.flush()
    return this
}