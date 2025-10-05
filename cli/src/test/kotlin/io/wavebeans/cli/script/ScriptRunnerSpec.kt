package io.wavebeans.cli.script

import assertk.assertThat
import assertk.assertions.*
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.wavebeans.execution.PodDiscovery
import io.wavebeans.execution.distributed.Facilitator
import io.wavebeans.lib.WaveBeansClassLoader
import io.wavebeans.tests.createPorts
import java.io.File
import java.lang.Thread.sleep
import java.util.concurrent.CancellationException

class ScriptRunnerSpec : DescribeSpec({
    isolationMode = IsolationMode.InstancePerLeaf

    val portRange: Array<Int> = createPorts(2)
    val facilitators = portRange
        .map {
            Facilitator(
                communicatorPort = it,
                threadsNumber = 2,
                onServerShutdownTimeoutMillis = 100,
                podDiscovery = object : PodDiscovery() {}
            )
        }

    facilitators.forEach { it.start() }

    afterSpec {
        facilitators.forEach {
            it.terminate()
            it.close()
        }
    }

    val modes = listOf(
        RunMode.DISTRIBUTED to mapOf(
            "partitions" to 2,
            "facilitatorLocations" to portRange.map { "127.0.0.1:$it" }),
        RunMode.MULTI_THREADED to mapOf<String, Any>("partitions" to 2, "threads" to 2),
        RunMode.LOCAL to emptyMap<String, Any>(),
    )

    fun Pair<RunMode, Map<String, Any>>.eval(script: String) =
        ScriptRunner(script, runMode = this.first, runOptions = this.second)
            .use { it.start().awaitForResult() }

    describe("Running scripts") {
        beforeTest {
            WaveBeansClassLoader.reset()
        }


        context("Short-living script with one output") {
            withData(modes) { mode ->
                val script = """
                        440.sine().map{ it }.trim(1).toDevNull().out()
                    """.trimIndent()
                assertThat(mode.eval(script)).isNull()
            }
        }

        context("Short-living script with multiple outputs") {
            withData(modes) { mode ->
                val script = """
                        440.sine().trim(1).toDevNull().out()
                        880.sine().trim(1).toDevNull().out()
                        1760.sine().trim(1).toDevNull().out()
                        3520.sine().trim(1).toDevNull().out()
                        7040.sine().trim(1).toDevNull().out()
                    """.trimIndent()

                assertThat(mode.eval(script)).isNull()
            }
        }

        context("Short-living script with more complicated logic") {
            withData(modes) { mode ->
                val file = File.createTempFile("test", ".csv").also { it.deleteOnExit() }
                val script = """
                    val i1 = 440.sine()
                    val i2 = 880.sine()

                    (i1 + i2).trim(1).toCsv("file://${file.absolutePath}").out()
                """.trimIndent()

                assertThat(mode.eval(script)).isNull()
                assertThat(file.readText()).isNotEmpty()
            }
        }

        context("Long-living script interruption") {
            withData(modes) { (runMode, runOptions) ->
                val file = File.createTempFile("test", ".csv").also { it.deleteOnExit() }
                val script = """
                        440.sine().map{ it }.trim(Long.MAX_VALUE).toCsv("file://${file.absolutePath}").out() // takes forever to finish
                    """.trimIndent()

                // This test uses sleeps, better to wait properly, test may become flaky
                ScriptRunner(script, runMode = runMode, runOptions = runOptions).start().use { runner ->

                    sleep(5000) // without that wait on fast machines script is not actually started but attempted to finish already.

                    assertThat(runner.result(), "not finished right after the start")
                        .prop("finished") { it.first }.isFalse()

                    assertThat(runner.interrupt(true), "there was something to interrupt")
                        .isTrue()

                    assertThat { runner.awaitForResult(timeout = 100) }
                        .isFailure()
                        .isInstanceOf(CancellationException::class)

                    runner.close()

                    // even if no samples were generated some headers will be there
                    assertThat(file.readText()).isNotEmpty()
                }
            }
        }

        context("Compile time error in script") {
            withData(modes) { mode ->
                val script = """
                        noSuchMethod()
                    """.trimIndent()

                assertThat { mode.eval(script) }
                    .isFailure()
                    .message().isNotNull().contains("noSuchMethod")
            }
        }

        context("Runtime error in script") {
            withData(modes) { mode ->
                val script = """
                        throw Exception("my exception")
                    """.trimIndent()

                assertThat(mode.eval(script))
                    .isNotNull()
                    .message().isNotNull().contains("my exception")
            }
        }

        context("Using imports") {
            context("one import") {
                withData(modes) { mode ->
                    val script = """
                            import java.util.Date

                            Date()
                        """.trimIndent()

                    assertThat(mode.eval(script)).isNull()
                }
            }
            context("several imports") {
                withData(modes) { mode ->
                    val script = """
                            import java.util.Date
                            import kotlin.collections.*

                            Date()
                            val a = emptyList<Int>()
                        """.trimIndent()

                    assertThat(mode.eval(script)).isNull()
                }
            }

            context("several imports in one line") {
                withData(modes) { mode ->
                    val script = """
                            import java.util.Date; import kotlin.collections.*

                            Date()
                            val a = emptyList<Int>()
                        """.trimIndent()

                    assertThat(mode.eval(script)).isNull()
                }
            }
        }

        context("Defining function as class") {
            withData(modes) { mode ->
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

                assertThat(mode.eval(script)).isNull()
            }

        }

        context("Defining function as lambda") {
            withData(modes) { mode ->
                val script = """
                        input { (i, _) -> sampleOf(i) }
                          .map { it }
                          .trim(1)
                          .toDevNull()
                          .out()
                    """.trimIndent()

                assertThat(mode.eval(script)).isNull()
            }

        }
    }
})
