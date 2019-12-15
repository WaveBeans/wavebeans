package io.wavebeans.execution

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.size
import io.wavebeans.execution.TopologySerializer.jsonPretty
import io.wavebeans.lib.io.magnitudeToCsv
import io.wavebeans.lib.io.phaseToCsv
import io.wavebeans.lib.io.sine
import io.wavebeans.lib.io.toCsv
import io.wavebeans.lib.rangeProjection
import io.wavebeans.lib.stream.changeAmplitude
import io.wavebeans.lib.stream.fft.fft
import io.wavebeans.lib.stream.fft.trim
import io.wavebeans.lib.stream.plus
import io.wavebeans.lib.stream.trim
import io.wavebeans.lib.stream.window.window
import mu.KotlinLogging
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.io.File
import java.lang.Thread.sleep
import kotlin.system.measureTimeMillis

@ExperimentalStdlibApi
object OverseerIntegrationSpec : Spek({

    val log = KotlinLogging.logger {}

    describe("Two outputs with different paths but same content") {
        val f1 = File.createTempFile("test", ".csv").also { it.deleteOnExit() }
        val f2 = File.createTempFile("test", ".csv").also { it.deleteOnExit() }
        val f3 = File.createTempFile("test", ".csv").also { it.deleteOnExit() }
        val f4 = File.createTempFile("test", ".csv").also { it.deleteOnExit() }

        val i1 = seqStream()
        val i2 = seqStream()

        val length = 50L

        val p1 = i1.changeAmplitude(1.0).rangeProjection(-100, length)
        val p2 = i2.changeAmplitude(0.0)

        val o1 = p1
                .trim(length)
                .toCsv("file://${f1.absolutePath}")
        val pp = p1 + p2
        val o2 = pp
                .trim(length)
                .toCsv("file://${f2.absolutePath}")
        val fft = pp
                .window(401)
                .fft(512)
                .trim(length)
        val o3 = fft.magnitudeToCsv("file://${f3.absolutePath}")
        val o4 = fft.phaseToCsv("file://${f4.absolutePath}")

        val topology = listOf(o1, o2, o3, o4).buildTopology()
                .partition(2)
                .groupBeans()

        log.debug { "Topology: ${TopologySerializer.serialize(topology, jsonPretty)}" }

        val overseer = Overseer()

        val timeToDeploy = measureTimeMillis {
            overseer.deployTopology(topology, 2)
            log.debug { "Topology deployed" }
        }
        val timeToProcess = measureTimeMillis {
            overseer.waitToFinish()
            log.debug { "Everything processed" }
        }
        val timeToFinalize = measureTimeMillis {
            overseer.close()
            log.debug { "Everything closed" }
        }

        val f1Content = f1.readLines()
        val f2Content = f2.readLines()
        val f3Content = f3.readLines()
        val f4Content = f4.readLines()
        it("should have non empty output. 1st file") { assertThat(f1Content).size().isGreaterThan(1) }
        it("should have non empty output. 2nd file") { assertThat(f2Content).size().isGreaterThan(1) }
        it("should have the same output") { assertThat(f1Content).isEqualTo(f2Content) }

        val localRunTime = measureTimeMillis {
            listOf(o1, o2, o3, o4)
                    .map { it.writer(44100.0f) }
                    .forEach {
                        while (it.write()) {
                            sleep(0)
                        }
                        it.close()
                    }
        }
        log.debug { "Local run finished" }

        val f1LocalContent = f1.readLines()
        it("should have the same size as local content [1]") { assertThat(f1Content.size).isEqualTo(f1LocalContent.size) }
        it("should have the same output as local content [1]") { assertThat(f1Content).isEqualTo(f1LocalContent) }
        val f2LocalContent = f2.readLines()
        it("should have the same size as local content [2]") { assertThat(f2Content.size).isEqualTo(f2LocalContent.size) }
        it("should have the same output as local content [2]") { assertThat(f2Content).isEqualTo(f2LocalContent) }
        val f3LocalContent = f3.readLines()
        it("should have the same size as local content [3]") { assertThat(f3Content.size).isEqualTo(f3LocalContent.size) }
        it("should have the same output as local content [3]") { assertThat(f3Content).isEqualTo(f3LocalContent) }
        val f4LocalContent = f4.readLines()
        it("should have the same size as local content [4]") { assertThat(f4Content.size).isEqualTo(f4LocalContent.size) }
        it("should have the same output as local content [4]") { assertThat(f4Content).isEqualTo(f4LocalContent) }

        log.debug {
            "Deploy took $timeToDeploy ms, processing took $timeToProcess ms, " +
                    "finalizing took $timeToFinalize ms, local run time is $localRunTime ms"
        }

    }
})