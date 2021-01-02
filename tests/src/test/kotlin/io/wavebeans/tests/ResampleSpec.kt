package io.wavebeans.tests

import assertk.all
import assertk.assertThat
import assertk.assertions.isNotEmpty
import io.wavebeans.lib.io.*
import io.wavebeans.lib.stream.*
import io.wavebeans.metrics.MetricService
import org.spekframework.spek2.Spek
import org.spekframework.spek2.lifecycle.CachingMode.TEST
import org.spekframework.spek2.style.specification.describe
import java.io.File
import kotlin.math.abs

object ResampleSpec : Spek({

    val ports = createPorts(2)
    val facilitatorLocations = listOf("localhost:${ports[0]}", "localhost:${ports[1]}")

    beforeEachTest {
        MetricService.reset()
    }

    beforeGroup {
        Thread { startFacilitator(ports[0]) }.start()
        Thread { startFacilitator(ports[1]) }.start()
        waitForFacilitatorToStart("localhost:${ports[0]}")
        waitForFacilitatorToStart("localhost:${ports[1]}")
    }

    afterGroup {
        terminateFacilitator("localhost:${ports[0]}")
        terminateFacilitator("localhost:${ports[1]}")
    }

    data class Param(
            val mode: String,
            val locateFacilitators: () -> List<String>,
            val evaluate: (StreamOutput<*>, Float, List<String>) -> Unit
    )

    val modes: List<Param> = listOf(
            Param("local", { emptyList() }) { o, sampleRate, _ ->
                o.evaluate(sampleRate)
            },
            Param("multi-threaded", { emptyList() }) { o, sampleRate, _ ->
                o.evaluateInMultiThreadedMode(sampleRate)
            },
            Param("distributed", { facilitatorLocations }) { o, sampleRate, facilitators ->
                o.evaluateInDistributedMode(sampleRate, facilitators)
            },
    )

    describe("resampling wav file") {

        val input by memoized {
            (440.sine() * 0.2).trim(1000)
        }

        val wavFile by memoized {
            val f = File.createTempFile("source", ".wav")
            input.toMono16bitWav("file://${f.absolutePath}").evaluate(11025.0f)
            wave("file://${f.absolutePath}")
        }

        val outputFile by memoized(TEST) { File.createTempFile("resample", ".wav") }
        val targetSampleRate = 43210.0f

        modes.forEach { (mode, locateFacilitators, evaluate) ->
            it("should perform in $mode mode") {
                val stream = wavFile.resample(to = 44100.0f)
                        .map { it } // add pointless map-operation to make sure the bean is partitioned
                        .resample(resampleFn = sincResampleFunc(128))
                        .toMono16bitWav("file://${outputFile.absolutePath}")

                evaluate(stream, targetSampleRate, locateFacilitators())

                val samples = input.toList(targetSampleRate)
                assertThat(wave("file://${outputFile.absolutePath}").toList(targetSampleRate, take = 10000)).all {
                    isNotEmpty()
                    isContainedBy(samples) {a, b -> abs(a - b) < 1e-2}
                }
            }
        }
    }
})