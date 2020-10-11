package io.wavebeans.cli

import assertk.assertThat
import assertk.assertions.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.ClientRequestException
import io.ktor.client.features.ServerResponseException
import io.ktor.client.request.get
import io.wavebeans.cli.WaveBeansCli.Companion.name
import io.wavebeans.cli.WaveBeansCli.Companion.options
import io.wavebeans.cli.script.RunMode
import io.wavebeans.execution.PodDiscovery
import io.wavebeans.execution.distributed.Facilitator
import io.wavebeans.lib.WaveBeansClassLoader
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.apache.commons.cli.DefaultParser
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.Suite
import org.spekframework.spek2.style.specification.describe
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintWriter
import java.lang.Thread.sleep
import java.net.ConnectException
import java.net.URL
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS

object WaveBeansCliSpec : Spek({

    beforeEachTest {
        WaveBeansClassLoader.reset()
    }

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

            it("should execute") { assertThat(cli.tryScriptExecution()).isTrue() }
            it("should generate non empty file") { assertThat(file.readText()).isNotEmpty() }
            it("should output time to console") {
                assertThat(String(out.toByteArray())).matches(Regex("\\d+\\.\\d+sec\\s*"))
            }
        }

        describe("Short-living from executed on multi-threaded environment") {
            val scriptFile = File.createTempFile("test", "kts").also { it.deleteOnExit() }
            val file = File.createTempFile("test", "csv").also { it.deleteOnExit() }
            scriptFile.writeBytes("440.sine().trim(1).toCsv(\"file://${file.absolutePath}\").out()".toByteArray())
            val out = ByteArrayOutputStream()
            val cli = WaveBeansCli(
                    cli = DefaultParser().parse(options, arrayOf(
                            name,
                            "--execute-file", scriptFile.absolutePath,
                            "--time",
                            "--run-mode", RunMode.MULTI_THREADED.id,
                            "--threads", "1",
                            "--partitions", "1"
                    )),
                    printer = PrintWriter(out)
            )

            it("should execute") { assertThat(cli.tryScriptExecution()).isTrue() }
            it("should generate non empty file") { assertThat(file.readLines()).size().isGreaterThan(1) }
            it("should output time to console") {
                assertThat(String(out.toByteArray())).matches(Regex("\\d+\\.\\d+sec\\s*"))
            }
        }

        describe("Short-living from executed on distributed environment") {

            val portRange = 40002..40003
            val gardeners = portRange.map {
                Facilitator(
                        communicatorPort = it,
                        threadsNumber = 2,
                        onServerShutdownTimeoutMillis = 100,
                        podDiscovery = object : PodDiscovery() {}
                )
            }

            beforeGroup {
                gardeners.forEach { it.start() }
            }

            afterGroup {
                gardeners.forEach {
                    it.terminate()
                    it.close()
                }
            }

            val scriptFile = File.createTempFile("test", "kts").also { it.deleteOnExit() }
            val file = File.createTempFile("test", "csv").also { it.deleteOnExit() }
            scriptFile.writeBytes("440.sine().map{ it }.trim(1).toCsv(\"file://${file.absolutePath}\").out()".toByteArray())
            val out = ByteArrayOutputStream()
            val cli = WaveBeansCli(
                    cli = DefaultParser().parse(options, arrayOf(
                            name,
                            "--execute-file", scriptFile.absolutePath,
                            "--time",
                            "--run-mode", RunMode.DISTRIBUTED.id,
                            "--partitions", "2",
                            "--facilitators", portRange.map { "127.0.0.1:$it" }.joinToString(",")
                    )),
                    printer = PrintWriter(out)
            )

            it("should execute") { assertThat(cli.tryScriptExecution()).isTrue() }
            it("should generate non empty file") { assertThat(file.readLines()).size().isGreaterThan(1) }
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
                            "--http", "12349",
                            "--http-wait", "1",
                            "--verbose"
                    )),
                    printer = PrintWriter(out)
            )

            assertHttpHandling(cli, out, 12349)
        }

        describe("HTTP API in distributed mode") {
            val portRange = 40004..40005
            val gardeners = portRange.map {
                Facilitator(
                        communicatorPort = it,
                        threadsNumber = 2,
                        onServerShutdownTimeoutMillis = 100,
                        podDiscovery = object : PodDiscovery() {}
                )
            }

            beforeGroup {
                gardeners.forEach { it.start() }
            }

            afterGroup {
                gardeners.forEach {
                    it.terminate()
                    it.close()
                }
            }

            val out = ByteArrayOutputStream()
            val cli = WaveBeansCli(
                    cli = DefaultParser().parse(options, arrayOf(
                            name,
                            "--execute", "440.sine().map { it }.trim(1000).toTable(\"table1\").out()",
                            "--http", "12350",
                            "--http-wait", "1",
                            "--http-communicator-port", "12348",
                            "--verbose",
                            "--run-mode", "distributed",
                            "--partitions", "2",
                            "--facilitators", portRange.joinToString(",") { "127.0.0.1:$it" }
                    )),
                    printer = PrintWriter(out)
            )

            assertHttpHandling(cli, out, 12350)
        }
    }
})

private fun Suite.assertHttpHandling(cli: WaveBeansCli, out: ByteArrayOutputStream, port: Int) {
    val log = KotlinLogging.logger {  }
    val pool = Executors.newSingleThreadExecutor()

    afterGroup { pool.shutdownNow() }

    val taskStarted = CountDownLatch(1)
    val result = pool.submit(Callable<List<String>> {
        fun result(): List<String> {
            return runBlocking {
                HttpClient(CIO).use { client ->
                    val response = client.get<String>(URL("http://localhost:$port/table/table1/last?interval=1.ms"))
                    response
                }
            }.split("[\\r\\n]+".toRegex()).filterNot { it.isEmpty() }
        }

        taskStarted.countDown()
        var ret: List<String>
        while (true) {
            try {
                ret = result()
                if (ret.isNotEmpty()) { // if nothing returned keep trying
                    break
                }
            } catch (e: ServerResponseException) {
                // the table is not created yet, keep trying
                sleep(1)
            } catch (e: ClientRequestException) {
                // the table is not created yet, keep trying
                sleep(1)
            } catch (e: ConnectException) {
                // the server is not started yet, keep trying
                sleep(1)
            }
        }
        ret
    })

    it("should execute") {
        taskStarted.await(5000, MILLISECONDS) // wait for task to actually start
        assertThat(cli.tryScriptExecution()).isTrue()
        log.info { "Script executed with output: ${String(out.toByteArray())}" }
    }
    it("should return some value") { assertThat(result.get(5, SECONDS)).isNotEmpty() }
    it("should output something to console") { assertThat(String(out.toByteArray())).isNotEmpty() }
}