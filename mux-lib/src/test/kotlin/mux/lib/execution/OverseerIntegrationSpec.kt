package mux.lib.execution

import assertk.assertThat
import assertk.assertions.isEqualTo
import mux.lib.io.sine
import mux.lib.io.toCsv
import mux.lib.stream.changeAmplitude
import mux.lib.stream.plus
import mux.lib.stream.trim
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.io.File
import kotlin.system.measureTimeMillis

@ExperimentalStdlibApi
object OverseerIntegrationSpec : Spek({

    describe("Two outputs with different paths but same content") {
        val f1 = File.createTempFile("test", ".csv").also { it.deleteOnExit() }
        val f2 = File.createTempFile("test", ".csv").also { it.deleteOnExit() }

        val i1 = 440.sine(0.5)
        val i2 = 800.sine(0.0)

        val p1 = i1.changeAmplitude(1.7)
        val p2 = i2.changeAmplitude(1.8)
                .rangeProjection(0, 1000)

        val o1 = p1
                .trim(1000)
                .toCsv("file://${f1.absolutePath}")
        val o2 = (p1 + p2)
                .trim(1000)
                .toCsv("file://${f2.absolutePath}")

        val topology = listOf(o1, o2).buildTopology()
        println("Topology: $topology")

        val overseer = Overseer()

        val timeToDeploy = measureTimeMillis {
            overseer
                    .deployTopology(topology)
        }
        val timeToProcess = measureTimeMillis {
            overseer.waitToFinish()
        }
        val timeToFinalize = measureTimeMillis {
            overseer.close()
        }

        it("should have the same output") { assertThat(f1.readText()).isEqualTo(f2.readText()) }

        val localRunTime = measureTimeMillis {
            listOf(o1, o2)
                    .map { Pair(it, it.writer(44100.0f)) }
                    .map { it.second.write(1000); it }
                    .forEach { it.first.close(); it.second.close() }
        }

        println("Deploy took $timeToDeploy ms, processing took $timeToProcess ms, " +
                "finalizing took $timeToFinalize ms, local run time is $localRunTime ms")

    }
})