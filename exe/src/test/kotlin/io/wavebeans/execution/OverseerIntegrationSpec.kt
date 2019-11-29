package io.wavebeans.execution

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.size
import io.wavebeans.execution.TopologySerializer.jsonPretty
import io.wavebeans.lib.io.toCsv
import io.wavebeans.lib.stream.changeAmplitude
import io.wavebeans.lib.stream.plus
import io.wavebeans.lib.stream.trim
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import org.spekframework.spek2.style.specification.xdescribe
import java.io.File
import java.lang.Thread.sleep
import kotlin.system.measureTimeMillis

@ExperimentalStdlibApi
object OverseerIntegrationSpec : Spek({

    describe("Two outputs with different paths but same content") {
        val f1 = File.createTempFile("test", ".csv").also { it.deleteOnExit() }
        val f2 = File.createTempFile("test", ".csv").also { it.deleteOnExit() }

        val i1 = seqStream()
        val i2 = seqStream()

        val p1 = i1.changeAmplitude(1.0)
        val p2 = i2.changeAmplitude(0.0)

        val o1 = p1
                .trim(50)
                .toCsv("file://${f1.absolutePath}")
        val o2 = (p1 + p2)
                .trim(50)
                .toCsv("file://${f2.absolutePath}")

        val topology = listOf(o1, o2).buildTopology()
                .partition(2)
                .groupBeans()

        println("Topology: ${TopologySerializer.serialize(topology, jsonPretty)}")

        val overseer = Overseer()

        val timeToDeploy = measureTimeMillis {
            overseer.deployTopology(topology, 1)
            println("Topology deployed")
        }
        val timeToProcess = measureTimeMillis {
            overseer.waitToFinish()
            println("Everything processed")
        }
        val timeToFinalize = measureTimeMillis {
            overseer.close()
            println("Everything closed")
        }

        val f1Content = f1.readLines()
        val f2Content = f2.readLines()
        it("should have non empty output. 1st file") { assertThat(f1Content).size().isGreaterThan(1) }
        it("should have non empty output. 2nd file") { assertThat(f2Content).size().isGreaterThan(1) }
        it("should have the same output") { assertThat(f1Content).isEqualTo(f2Content) }

        val localRunTime = measureTimeMillis {
            listOf(o1, o2)
                    .map { it.writer(44100.0f) }
                    .forEach {
                        while (it.write()) {
                            sleep(0)
                        }
                        it.close()
                    }
        }
        println("Local run finished")

        val f1LocalContent = f1.readLines()
        it("should have the same size as local content") { assertThat(f1Content.size).isEqualTo(f1LocalContent.size) }
        it("should have the same output as local content") { assertThat(f1Content).isEqualTo(f1LocalContent) }
        val f2LocalContent = f2.readLines()
        it("should have the same size as local content") { assertThat(f2Content.size).isEqualTo(f2LocalContent.size) }
        it("should have the same output as local content") { assertThat(f2Content).isEqualTo(f2LocalContent) }

        println("Deploy took $timeToDeploy ms, processing took $timeToProcess ms, " +
                "finalizing took $timeToFinalize ms, local run time is $localRunTime ms")

    }
})