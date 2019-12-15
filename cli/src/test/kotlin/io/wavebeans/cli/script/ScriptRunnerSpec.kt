package io.wavebeans.cli.script

import assertk.assertThat
import assertk.assertions.*
import assertk.catch
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.io.File
import java.lang.Thread.sleep
import java.util.concurrent.CancellationException

object ScriptRunnerSpec : Spek({

    arrayOf(
            Pair(RunMode.LOCAL, emptyMap()),
            Pair(RunMode.LOCAL_DISTRIBUTED, mapOf<String, Any>("partitions" to 2, "threads" to 2))
    ).forEach { (runMode, runOptions) ->

        describe("Running in $runMode mode with options=$runOptions") {

            fun eval(script: String) = ScriptRunner(script, runMode = runMode, runOptions = runOptions).use {
                it.start().awaitForResult()
            }

            describe("Short-living script with one output") {
                val script = """
                    440.sine().trim(1).toDevNull().out()
                """.trimIndent()

                it("should run with no exceptions") { assertThat(eval(script)).isNull() }
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
                    440.sine().trim(Long.MAX_VALUE).toCsv("file://${file.absolutePath}").out() // takes forever to finish
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
                    assertThat(eval(script))
                            .isNotNull()
                            .message().isNotNull().contains("noSuchMethod()")
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
        }
    }
})
