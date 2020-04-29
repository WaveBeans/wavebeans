package io.wavebeans.execution.distributed

import assertk.all
import assertk.assertThat
import assertk.assertions.*
import assertk.assertions.support.fail
import io.wavebeans.execution.SingleThreadedOverseer
import io.wavebeans.execution.eachIndexed
import io.wavebeans.execution.newJobKey
import io.wavebeans.lib.io.magnitudeToCsv
import io.wavebeans.lib.io.sine
import io.wavebeans.lib.io.toCsv
import io.wavebeans.lib.stream.fft.fft
import io.wavebeans.lib.stream.map
import io.wavebeans.lib.stream.plus
import io.wavebeans.lib.stream.trim
import io.wavebeans.lib.stream.window.hamming
import io.wavebeans.lib.stream.window.window
import mu.KotlinLogging
import org.spekframework.spek2.Spek
import org.spekframework.spek2.lifecycle.CachingMode.*
import org.spekframework.spek2.style.specification.describe
import java.io.File
import java.io.IOException
import java.lang.Thread.sleep
import java.util.concurrent.Executors
import kotlin.math.abs

object DistributedOverseerSpec : Spek({

    val log = KotlinLogging.logger {}
    val pool = Executors.newCachedThreadPool()

    pool.submit { startCrewGardener(40001) }
    pool.submit { startCrewGardener(40002) }

    val crewGardenersLocations = listOf("http://127.0.0.1:40001", "http://127.0.0.1:40002")

    crewGardenersLocations.forEach {
        while (true) {
            try {
                if (CrewGardenerService.create(it).status().execute().isSuccessful)
                    break
            } catch (e: IOException) {
                // continue trying
            }
            sleep(1)
        }
    }

    afterGroup {
        try {
            crewGardenersLocations.forEach { location ->
                CrewGardenerService.create(location).terminate()
            }
        } finally {
            pool.shutdownNow()
        }
    }

    describe("Running code accessible by Crew Gardener without extra loading") {
        val outputs = {
            val file1 = File.createTempFile("test", ".csv").also { it.deleteOnExit() }
            val file2 = File.createTempFile("test", ".csv").also { it.deleteOnExit() }
            val input = 440.sine().map { abs(it) } + 880.sine()
            val output1 = input
                    .trim(500)
                    .toCsv("file:///${file1.absolutePath}")
            val output2 = input
                    .window(101, 25)
                    .hamming()
                    .fft(128)
                    .trim(500)
                    .magnitudeToCsv("file:///${file2.absolutePath}")
            listOf(output1 to file1, output2 to file2)
        }

        val outputsDistributed = outputs()
        val distributed = DistributedOverseer(
                outputsDistributed.map { it.first },
                crewGardenersLocations,
                2
        )

        val outputsSingle = outputs()
        val single = SingleThreadedOverseer(outputsSingle.map { it.first })

        it("shouldn't throw any exceptions", timeout = 0) {
            val exceptions = distributed.eval(44100.0f).mapNotNull { it.get().exception }
            distributed.close()
            assertThat(exceptions).isEmpty()
        }
        it("shouldn't throw any exceptions") {
            val exceptions = single.eval(44100.0f).mapNotNull { it.get().exception }
            single.close()
            assertThat(exceptions).isEmpty()
        }
        it("should have the same output") {
            assertThat(outputsDistributed[0].second.readLines()).all {
                size().isGreaterThan(1)
                isEqualTo(outputsSingle[0].second.readLines())
            }
            assertThat(outputsDistributed[1].second.readLines()).all {
                size().isGreaterThan(1)
                isEqualTo(outputsSingle[1].second.readLines())
            }
        }
    }

    describe("Running code not accessible by Crew Gardener") {
        val file = File.createTempFile("testAppOut", ".csv")

        fun codeFile(name: String): Pair<String, String> {
            return name to this::class.java.getResourceAsStream("/testApp/$name").reader().readText()
                    .replace(
                            "/[*]FILE[*]/.*/[*]FILE[*]/".toRegex(),
                            "File(\"${file.absolutePath}\")"
                    )
                    .replace(
                            "/[*]CREW_GARDENER_LIST[*]/.*/[*]CREW_GARDENER_LIST[*]/".toRegex(),
                            "listOf(\"http://127.0.0.1:40001\", \"http://127.0.0.1:40002\")"
                    )
                    .also { log.info { "$name:\n$it" } }
        }

        fun extractExceptions(runCall: CommandResult): List<String> {
            return String(runCall.output)
                    .replace(
                            ".*>>>> EXCEPTIONS(.*)<<<< EXCEPTIONS.*".toRegex(RegexOption.DOT_MATCHES_ALL),
                            "$1"
                    )
                    .trim()
                    .split("----SPLITTER----")
                    .filter { it.isNotEmpty() }
        }

        val jarFile by memoized(SCOPE) {
            compileCode(mapOf(
                    codeFile("Runner.kt"),
                    codeFile("Success.kt"),
                    codeFile("Error.kt")
            ))
        }

        describe("Successful runner") {
            val runner by memoized(SCOPE) {
                CommandRunner(
                        javaCmd(),
                        "-cp", System.getProperty("java.class.path") + ":" + jarFile.absolutePath,
                        "io.wavebeans.test.app.SuccessKt"
                )
            }

            lateinit var runCall: CommandResult
            it("should execute") {
                runCall = runner.run(inheritIO = false)
                assertThat(runCall.exitCode).isEqualTo(0)
            }
            it("shouldn't throw any exceptions to output") {
                val exceptions = extractExceptions(runCall)
                assertThat(exceptions).isEmpty()
            }
            it("should contain 11 rows in output file") {
                assertThat(file.readLines()).all {
                    eachIndexed(11) { r, idx ->
                        when (idx) {
                            0 -> r.isEqualTo("index,value")
                            in 1..10 -> r.isEqualTo("${idx - 1},${idx - 1}")
                            else -> r.fail("index < 11", "index=$idx")
                        }
                    }
                }
            }
        }

        describe("Error runner") {
            val runner by memoized(SCOPE) {
                CommandRunner(
                        javaCmd(),
                        "-cp", System.getProperty("java.class.path") + ":" + jarFile.absolutePath,
                        "io.wavebeans.test.app.ErrorKt"
                )
            }

            lateinit var runCall: CommandResult
            it("should execute") {
                runCall = runner.run(inheritIO = false)
                assertThat(runCall.exitCode).isEqualTo(0)
            }
            it("should throw two exceptions to output") {
                val exceptions = extractExceptions(runCall)
                assertThat(exceptions).size().isEqualTo(2)
            }
        }
    }
})

