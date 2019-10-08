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
                .trim(10)
                .toCsv("file://${f1.absolutePath}")
        val o2 = (p1 + p2)
                .trim(10)
                .toCsv("file://${f2.absolutePath}")

        val topology = listOf(o1, o2).buildTopology()
        println("Topology: $topology")

        // there are tick pods on the topology, they'll pick up and start working
        val time = measureTimeMillis {
            Overseer()
                    .deployTopology(topology)
                    .waitToFinish()
                    .close()
        }

        println("Took $time ms")

        it("should have the same output") { assertThat(f1.readText()).isEqualTo(f2.readText()) }

    }
})