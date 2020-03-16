package io.wavebeans.cli

import assertk.assertThat
import assertk.assertions.isNotEmpty
import assertk.assertions.isTrue
import assertk.assertions.matches
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.ClientRequestException
import io.ktor.client.request.get
import io.wavebeans.cli.WaveBeansCli.Companion.name
import io.wavebeans.cli.WaveBeansCli.Companion.options
import kotlinx.coroutines.runBlocking
import org.apache.commons.cli.DefaultParser
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintWriter
import java.lang.Thread.sleep
import java.net.URL
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.SECONDS

@ExperimentalStdlibApi
object WaveBeansCliSpec : Spek({
    describe("Scripting") {
        describe("Short-living script") {
            val file = File.createTempFile("test", "csv").also { it.deleteOnExit() }
            val out = ByteArrayOutputStream()
            val cli = WaveBeansCli(
                    cli = DefaultParser().parse(options, arrayOf(
                            name,
                            "--execute", "440.sine().trim(1).toCsv(\"file://${file.absolutePath}\").out()",
                            "--verbose"
                    )),
                    printer = PrintWriter(out)
            )
            out.flush()
            out.close()

            it("should execute") { assertThat(cli.tryScriptExecution()).isTrue() }
            it("should generate non empty file") { assertThat(file.readText()).isNotEmpty() }
            it("should output something to console") { assertThat(String(out.toByteArray())).isNotEmpty() }
        }

        describe("Short-living from file") {
            val scriptFile = File.createTempFile("test", "kts").also { it.deleteOnExit() }
            val file = File.createTempFile("test", "csv").also { it.deleteOnExit() }
            scriptFile.writeBytes("440.sine().trim(1).toCsv(\"file://${file.absolutePath}\").out()".toByteArray())
            val out = ByteArrayOutputStream()
            val cli = WaveBeansCli(
                    cli = DefaultParser().parse(options, arrayOf(
                            name,
                            "--execute-file", scriptFile.absolutePath,
                            "--time"
                    )),
                    printer = PrintWriter(out)
            )
            out.flush()
            out.close()

            it("should execute") { assertThat(cli.tryScriptExecution()).isTrue() }
            it("should generate non empty file") { assertThat(file.readText()).isNotEmpty() }
            it("should output time to console") {
                assertThat(String(out.toByteArray())).matches(Regex("\\d+\\.\\d+sec\\s*"))
            }
        }

        describe("Short-living from executed on distributed environment") {
            val scriptFile = File.createTempFile("test", "kts").also { it.deleteOnExit() }
            val file = File.createTempFile("test", "csv").also { it.deleteOnExit() }
            scriptFile.writeBytes("440.sine().trim(1).toCsv(\"file://${file.absolutePath}\").out()".toByteArray())
            val out = ByteArrayOutputStream()
            val cli = WaveBeansCli(
                    cli = DefaultParser().parse(options, arrayOf(
                            name,
                            "--execute-file", scriptFile.absolutePath,
                            "--time",
                            "--run-mode", "local-distributed",
                            "--threads", "1",
                            "--partitions", "1"
                    )),
                    printer = PrintWriter(out)
            )
            out.flush()
            out.close()

            it("should execute") { assertThat(cli.tryScriptExecution()).isTrue() }
            it("should generate non empty file") { assertThat(file.readText()).isNotEmpty() }
            it("should output time to console") {
                assertThat(String(out.toByteArray())).matches(Regex("\\d+\\.\\d+sec\\s*"))
            }
        }

        describe("HTTP API") {
            val out = ByteArrayOutputStream()
            val cli = WaveBeansCli(
                    cli = DefaultParser().parse(options, arrayOf(
                            name,
                            "--execute", "440.sine().trim(1000).toTable(\"table1\").out()",
                            "--http", "12345",
                            "--http-wait", "0",
                            "--verbose"
                    )),
                    printer = PrintWriter(out)
            )
            out.flush()
            out.close()

            val pool = Executors.newSingleThreadExecutor()
            after { pool.shutdownNow() }
            val result = pool.submit(Callable<List<String>> {
                fun result(): List<String> {
                    return runBlocking {
                        HttpClient(CIO).use { client ->
                            val response = client.get<String>(URL("http://localhost:12345/table/table1/last?interval=1.ms"))
                            response
                        }
                    }.split("[\\r\\n]+".toRegex()).filterNot { it.isEmpty() }
                }

                var ret: List<String>
                while (true) {
                    try {
                        ret = result()
                        if (ret.isNotEmpty()) { // if nothing returned keep trying
                            break
                        }
                    } catch (e: ClientRequestException) {
                        // the table is not created yet, keep trying
                        sleep(1)
                    } catch (e: java.net.ConnectException) {
                        // the server is not started yet, keep trying
                        sleep(1)
                    }
                }
                ret
            })

            it("should execute") { assertThat(cli.tryScriptExecution()).isTrue() }
            it("should return some value") { assertThat(result.get(1, SECONDS)).isNotEmpty() }
            it("should output something to console") { assertThat(String(out.toByteArray())).isNotEmpty() }

        }
    }
})