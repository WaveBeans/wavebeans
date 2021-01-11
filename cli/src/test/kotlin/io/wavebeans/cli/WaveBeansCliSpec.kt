package io.wavebeans.cli

import assertk.assertThat
import assertk.assertions.*
import io.wavebeans.cli.WaveBeansCli.Companion.name
import io.wavebeans.cli.WaveBeansCli.Companion.options
import io.wavebeans.cli.script.RunMode
import io.wavebeans.execution.PodDiscovery
import io.wavebeans.execution.distributed.Facilitator
import io.wavebeans.lib.WaveBeansClassLoader
import io.wavebeans.tests.createPorts
import io.wavebeans.tests.findFreePort
import mu.KotlinLogging
import org.apache.commons.cli.DefaultParser
import org.http4k.client.OkHttp
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.Suite
import org.spekframework.spek2.style.specification.describe
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintWriter
import java.lang.Thread.sleep
import java.net.ConnectException
import java.net.SocketException
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
                cli = DefaultParser().parse(
                    options, arrayOf(
                        name,
                        "--execute", "440.sine().trim(1).toCsv(\"file://${file.absolutePath}\").out()",
                        "--verbose"
                    )
                ),
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
                cli = DefaultParser().parse(
                    options, arrayOf(
                        name,
                        "--execute-file", scriptFile.absolutePath,
                        "--time"
                    )
                ),
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
                cli = DefaultParser().parse(
                    options, arrayOf(
                        name,
                        "--execute-file", scriptFile.absolutePath,
                        "--time",
                        "--run-mode", RunMode.MULTI_THREADED.id,
                        "--threads", "1",
                        "--partitions", "1"
                    )
                ),
                printer = PrintWriter(out)
            )

            it("should execute") { assertThat(cli.tryScriptExecution()).isTrue() }
            it("should generate non empty file") { assertThat(file.readLines()).size().isGreaterThan(1) }
            it("should output time to console") {
                assertThat(String(out.toByteArray())).matches(Regex("\\d+\\.\\d+sec\\s*"))
            }
        }

        describe("Short-living from executed on distributed environment") {

            val portRange = createPorts(2)
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
                cli = DefaultParser().parse(
                    options, arrayOf(
                        name,
                        "--execute-file", scriptFile.absolutePath,
                        "--time",
                        "--run-mode", RunMode.DISTRIBUTED.id,
                        "--partitions", "2",
                        "--facilitators", portRange.map { "127.0.0.1:$it" }.joinToString(",")
                    )
                ),
                printer = PrintWriter(out)
            )

            it("should execute") { assertThat(cli.tryScriptExecution()).isTrue() }
            it("should generate non empty file") { assertThat(file.readLines()).size().isGreaterThan(1) }
            it("should output time to console") {
                assertThat(String(out.toByteArray())).matches(Regex("\\d+\\.\\d+sec\\s*"))
            }
        }

        describe("HTTP API") {
            val httpPort = findFreePort()
            val out = ByteArrayOutputStream()
            val cli = WaveBeansCli(
                cli = DefaultParser().parse(
                    options, arrayOf(
                        name,
                        "--execute", "440.sine().trim(1000).toTable(\"table1\").out()",
                        "--http", "$httpPort",
                        "--http-wait", "1",
                        "--verbose"
                    )
                ),
                printer = PrintWriter(out)
            )

            assertHttpHandling(cli, out, httpPort)
        }

        describe("HTTP API in distributed mode") {
            val portRange = createPorts(2)
            val httpPort = findFreePort()
            val httpCommunicatorPort = findFreePort()
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
                    "--http", "$httpPort",
                    "--http-wait", "1",
                    "--http-communicator-port", "$httpCommunicatorPort",
                    "--verbose",
                    "--run-mode", "distributed",
                    "--partitions", "2",
                    "--facilitators", portRange.joinToString(",") { "127.0.0.1:$it" }
                )),
                printer = PrintWriter(out)
            )

            assertHttpHandling(cli, out, httpPort)
        }
    }
})

private fun Suite.assertHttpHandling(cli: WaveBeansCli, out: ByteArrayOutputStream, port: Int) {
    val log = KotlinLogging.logger { }
    val pool = Executors.newSingleThreadExecutor()

    afterGroup { pool.shutdownNow() }

    val taskStarted = CountDownLatch(1)
    val result = pool.submit(Callable {
        fun result(): Response {
            val client = OkHttp()
            return client(Request(Method.GET, "http://localhost:$port/table/table1/last?interval=1.ms"))
        }

        taskStarted.countDown()
        var ret: List<String>
        while (true) {
            sleep(500)
            try {
                val r = result()
                if (r.status == Status.OK) { // if nothing returned keep trying
                    ret = r
                        .bodyString()
                        .split("[\\r\\n]+".toRegex())
                        .filterNot { it.isEmpty() }
                    log.debug { "The API finally returned $ret" }
                    break
                }
            } catch (e: Exception) {
                if (e !is ConnectException && e !is SocketException) throw e
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