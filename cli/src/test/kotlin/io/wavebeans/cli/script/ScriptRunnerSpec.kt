package io.wavebeans.cli.script

import assertk.assertThat
import assertk.assertions.*
import assertk.catch
import io.wavebeans.execution.PodDiscovery
import io.wavebeans.execution.distributed.Facilitator
import io.wavebeans.lib.WaveBeansClassLoader
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.io.File
import java.lang.Thread.sleep
import java.util.concurrent.CancellationException

object ScriptRunnerSpec : Spek({

    val portRange = 40000..40001
    val gardeners = portRange.map {
        Facilitator(
                communicatorPort = it,
                threadsNumber = 2,
                onServerShutdownTimeoutMillis = 100,
                podDiscovery = object : PodDiscovery() {}
        )
    }

    beforeEachTest {
        WaveBeansClassLoader.reset()
    }

    arrayOf(
            Pair(RunMode.DISTRIBUTED, mapOf("partitions" to 2, "facilitatorLocations" to portRange.map { "127.0.0.1:$it" })),
            Pair(RunMode.MULTI_THREADED, mapOf<String, Any>("partitions" to 2, "threads" to 2)),
            Pair(RunMode.LOCAL, emptyMap<String, Any>())
    ).forEach { (runMode, runOptions) ->

        describe("Running in $runMode mode with options=$runOptions") {

            beforeGroup {
                if (runMode == RunMode.DISTRIBUTED)
                    gardeners.forEach { it.start() }
            }

            afterGroup {
                if (runMode == RunMode.DISTRIBUTED)
                    gardeners.forEach {
                        it.terminate()
                        it.close()
                    }
            }

            fun eval(script: String) = ScriptRunner(script, runMode = runMode, runOptions = runOptions).use {
                it.start().awaitForResult()
            }

            describe("Short-living script with one output") {
                val script = """
                    440.sine().map{ it }.trim(1).toDevNull().out()
                """.trimIndent()

                it("should run with no exceptions") {
                    assertThat(eval(script)).isNull()
                }
            }

            describe("Short-living script with multiple outputs") {
                val script = """
                    440.sine().trim(1).toDevNull().out()
                    880.sine().trim(1).toDevNull().out()
                    1760.sine().trim(1).toDevNull().out()
                    3520.sine().trim(1).toDevNull().out()
                    7040.sine().trim(1).toDevNull().out()
                """.trimIndent()

                it("should run with no exceptions") { assertThat(eval(script)).isNull() }
            }

            describe("Short-living script with more complicated logic") {
                val file = File.createTempFile("test", ".csv").also { it.deleteOnExit() }
                val script = """
                    val i1 = 440.sine()
                    val i2 = 880.sine()

                    (i1 + i2).trim(1).toCsv("file://${file.absolutePath}").out()
                """.trimIndent()

                it("should run with no exceptions") { assertThat(eval(script)).isNull() }
                it("should generate non-empty file") { assertThat(file.readText()).isNotEmpty() }
            }

            describe("Long-living script interruption") {
                val file = File.createTempFile("test", ".csv").also { it.deleteOnExit() }
                val script = """
                    440.sine().map{ it }.trim(Long.MAX_VALUE).toCsv("file://${file.absolutePath}").out() // takes forever to finish
                """.trimIndent()

                it("should return something after interruption") {
                    // This test uses sleeps, better to wait properly, test may become flaky
                    ScriptRunner(script, runMode = runMode, runOptions = runOptions).start().use { runner ->

                        sleep(5000) // without that wait on fast machines script is not actually started but attempted to finish already.

                        assertThat(runner.result(), "not finished right after the start")
                                .prop("finished") { it.first }.isFalse()

                        assertThat(runner.interrupt(true), "there was something to interrupt")
                                .isTrue()

                        val e = catch { runner.awaitForResult(timeout = 100) }
                        assertThat(e)
                                .isNotNull()
                                .isInstanceOf(CancellationException::class)

                        runner.close()

                        // even if no samples were generated some headers will be there
                        assertThat(file.readText()).isNotEmpty()
                    }
                }
            }

            describe("Compile time error in script") {
                val script = """
                    noSuchMethod()
                """.trimIndent()

                it("should run with exception explaining the reason") {
                    assertThat(catch { eval(script) })
                            .isNotNull()
                            .message().isNotNull().contains("noSuchMethod")
                }
            }

            describe("Runtime error in script") {
                val script = """
                    throw Exception("my exception")
                """.trimIndent()

                it("should run with exception explaining the reason") {
                    assertThat(eval(script))
                            .isNotNull()
                            .message().isNotNull().contains("my exception")
                }
            }

            describe("Using imports") {
                describe("one import") {
                    val script = """
                        import java.util.Date

                        Date()
                    """.trimIndent()

                    it("should run with no exceptions") {
                        assertThat(eval(script)).isNull()
                    }
                }
                describe("several imports") {
                    val script = """
                        import java.util.Date
                        import kotlin.collections.*

                        Date()
                        val a = emptyList<Int>()
                    """.trimIndent()

                    it("should run with no exceptions") {
                        assertThat(eval(script)).isNull()
                    }
                }

                describe("several imports in one line") {
                    val script = """
                        import java.util.Date; import kotlin.collections.*

                        Date()
                        val a = emptyList<Int>()
                    """.trimIndent()

                    it("should run with no exceptions") {
                        assertThat(eval(script)).isNull()
                    }
                }
            }

            describe("Defining function as class") {
                val script = """
                    class InputFn: Fn<Pair<Long, Float>, Sample?>() {
                        override fun apply(argument: Pair<Long, Float>): Sample? {
                            return sampleOf(argument.first)
                        }
                    }

                    input(InputFn())
                      .map { it }
                      .trim(1)
                      .toDevNull()
                      .out()
                """.trimIndent()

                it("should run with no exceptions") {
                    assertThat(eval(script)).isNull()
                }

            }

            describe("Defining function as lambda") {
                val script = """
                    input { (i, _) -> sampleOf(i) }
                      .map { it }
                      .trim(1)
                      .toDevNull()
                      .out()
                """.trimIndent()

                it("should run with no exceptions") {
                    assertThat(eval(script)).isNull()
                }

            }
        }
    }
})
