package io.wavebeans.tests

import assertk.assertThat
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.wavebeans.fs.local.LocalWbFileDriver
import io.wavebeans.lib.io.StreamOutput
import io.wavebeans.lib.io.WbFileDriver
import io.wavebeans.lib.io.sine
import io.wavebeans.lib.io.toMono16bitWav
import io.wavebeans.lib.io.wave
import io.wavebeans.lib.stream.*
import io.wavebeans.lib.stream.fft.fft
import io.wavebeans.lib.stream.fft.inverseFft
import io.wavebeans.lib.stream.window.window
import io.wavebeans.metrics.MetricService
import java.io.File
import kotlin.math.abs

class FlattenSpec : DescribeSpec({

    lateinit var outputFile: File
    val ports = createPorts(2)
    val facilitatorLocations = listOf("localhost:${ports[0]}", "localhost:${ports[1]}")

    beforeTest {
        MetricService.reset()
        outputFile = File.createTempFile("tmp", ".wav")
    }

    beforeSpec {
        Thread { startFacilitator(ports[0]) }.start()
        Thread { startFacilitator(ports[1]) }.start()
        waitForFacilitatorToStart("localhost:${ports[0]}")
        waitForFacilitatorToStart("localhost:${ports[1]}")
        WbFileDriver.registerDriver("file", LocalWbFileDriver)
    }

    afterSpec {
        terminateFacilitator("localhost:${ports[0]}")
        terminateFacilitator("localhost:${ports[1]}")
        WbFileDriver.unregisterDriver("file")
    }

    data class Param(
        val locateFacilitators: () -> List<String>,
        val evaluate: (StreamOutput<*>, Float, List<String>) -> Unit
    )

    val modes: Map<String, Param> = mapOf(
        "local" to Param({ emptyList() }) { o, sampleRate, _ ->
            o.evaluate(sampleRate)
        },
        "multi-threaded" to Param({ emptyList() }) { o, sampleRate, _ ->
            o.evaluateInMultiThreadedMode(sampleRate)
        },
        "distributed" to Param({ facilitatorLocations }) { o, sampleRate, facilitators ->
            o.evaluateInDistributedMode(sampleRate, facilitators)
        },
    )
    describe("Using FFT to tune the signal and restoring back after the processing") {
        withData(modes) { (locateFacilitators, evaluate) ->
            val lengthMs = 1000L
            val sampleRate = 44100.0f

            val input = (120.sine() + 240.sine() + 350.sine() + 40.sine() + 80.sine()) * 0.2
            val o = input.trim(lengthMs * 2)
                .window(1001, 501)
                .fft(2048)
                .inverseFft()
                .flatten { (a, _) -> a }
                .trim(lengthMs)
                .toMono16bitWav("file://${outputFile.absolutePath}")

            evaluate(o, sampleRate, locateFacilitators())

            val expected = input.trim(lengthMs).asSequence(sampleRate).toList()
            val actual =
                (wave("file://${outputFile.absolutePath}")).asSequence(sampleRate).drop(1000).take(10000).toList()
            assertThat(actual).isContainedBy(expected) { a, b -> abs(a - b) < 1e-4 }
        }
    }

    describe("Smoothing the signal") {
        withData(modes) { (locateFacilitators, evaluate) ->
            val lengthMs = 100L
            val sampleRate = 400.0f

            val input = (120.sine() + 40.sine() + 80.sine()) * 0.2
            val o = input.trim(lengthMs * 2)
                .window(4)
                .map {
                    val a = it.elements.average()
                    (0 until it.size).map { a }
                }
                .flatten()
                .trim(lengthMs)
                .toMono16bitWav("file://${outputFile.absolutePath}")

            evaluate(o, sampleRate, locateFacilitators())

            val expected = input.trim(lengthMs).asSequence(sampleRate)
                .windowed(4, 4, partialWindows = true)
                .flatMap {
                    val a = it.average()
                    it.map { a }
                }
                .toList()
            val actual = (wave("file://${outputFile.absolutePath}")).asSequence(sampleRate).toList()
            assertThat(actual).isContainedBy(expected) { a, b -> abs(a - b) < 1e-4 }
        }
    }
})