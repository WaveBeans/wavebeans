package io.wavebeans.tests

import assertk.all
import assertk.assertThat
import assertk.assertions.isNotEmpty
import io.kotest.core.spec.style.DescribeSpec
import io.wavebeans.lib.io.StreamOutput
import io.wavebeans.lib.io.sine
import io.wavebeans.lib.io.toMono16bitWav
import io.wavebeans.lib.io.wave
import io.wavebeans.lib.stream.*
import io.wavebeans.metrics.MetricService
import java.io.File
import kotlin.math.abs

class ResampleSpec : DescribeSpec({

    val ports = createPorts(2)
    val facilitatorLocations = listOf("localhost:${ports[0]}", "localhost:${ports[1]}")

    beforeTest {
        MetricService.reset()
    }

    beforeSpec {
        Thread { startFacilitator(ports[0]) }.start()
        Thread { startFacilitator(ports[1]) }.start()
        waitForFacilitatorToStart("localhost:${ports[0]}")
        waitForFacilitatorToStart("localhost:${ports[1]}")
    }

    afterSpec {
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

        val input = (440.sine() * 0.2).trim(1000)

        val wavFile by lazy {
            val f = File.createTempFile("source", ".wav")
            input.toMono16bitWav("file://${f.absolutePath}").evaluate(11025.0f)
            wave("file://${f.absolutePath}")
        }

        lateinit var outputFile: File
        val targetSampleRate = 43210.0f

        beforeTest {
            outputFile = File.createTempFile("resample", ".wav")
        }

        modes.forEach { (mode, locateFacilitators, evaluate) ->
            it("should perform in $mode mode") {
                val stream = wavFile.resample(to = 44100.0f)
                    .map { it } // add pointless map-operation to make sure the bean is partitioned
                    .resample(resampleFn = { sincResampleFunc(128)(it) })
                    .toMono16bitWav("file://${outputFile.absolutePath}")

                evaluate(stream, targetSampleRate, locateFacilitators())

                val samples = input.toList(targetSampleRate)
                assertThat(wave("file://${outputFile.absolutePath}").toList(targetSampleRate, take = 10000)).all {
                    isNotEmpty()
                    isContainedBy(samples) { a, b -> abs(a - b) < 1e-2 }
                }
            }
        }
    }
})