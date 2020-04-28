package io.wavebeans.execution.distributed

import assertk.all
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.size
import io.wavebeans.execution.SingleThreadedOverseer
import io.wavebeans.lib.io.magnitudeToCsv
import io.wavebeans.lib.io.sine
import io.wavebeans.lib.io.toCsv
import io.wavebeans.lib.stream.fft.fft
import io.wavebeans.lib.stream.map
import io.wavebeans.lib.stream.plus
import io.wavebeans.lib.stream.trim
import io.wavebeans.lib.stream.window.hamming
import io.wavebeans.lib.stream.window.window
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.io.File
import java.util.concurrent.Executors
import kotlin.math.abs

object DistributedOverseerSpec : Spek({

    val pool = Executors.newCachedThreadPool()

    pool.submit { startCrewGardener(40001) }
    pool.submit { startCrewGardener(40002) }
    Thread.sleep(5000) // wait for the start, need to get rid of it

    val crewGardenersLocations = listOf("http://127.0.0.1:40001", "http://127.0.0.1:40002")
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
})

fun startCrewGardener(port: Int) {
    val confFile = File.createTempFile("crew-gardener-config", ".conf").also { it.deleteOnExit() }
    confFile.writeText("""
        crewGardenderConfig {
            advertisingHostAddress: 127.0.0.1
            listeningPortRange: {start: $port, end: $port}
            threadsNumber: 1
        }
    """.trimIndent())

    val java = "java"
    val javaHome = System.getenv("JAVA_HOME")
            ?.takeIf { File("$it/$java").exists() }
            ?: System.getenv("PATH")
                    .split(":")
                    .firstOrNull { File("$it/$java").exists() }
            ?: throw IllegalStateException("$java is not located, make sure it is available via either" +
                    " PATH or JAVA_HOME environment variable")


    val runner = CommandRunner(
            "$javaHome/$java",
            "-cp", System.getProperty("java.class.path"),
            "io.wavebeans.execution.distributed.CrewGardenerKt", confFile.absolutePath
    )
    val runCall = runner.call()

    if (runCall.exitCode != 0) {
        throw IllegalStateException("Can't compile the script. \n${String(runCall.output)}")
    }

}