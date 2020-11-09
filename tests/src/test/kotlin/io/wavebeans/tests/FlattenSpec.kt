package io.wavebeans.tests

import assertk.assertThat
import io.wavebeans.lib.io.StreamOutput
import io.wavebeans.lib.io.sine
import io.wavebeans.lib.io.toMono16bitWav
import io.wavebeans.lib.io.wave
import io.wavebeans.lib.math.r
import io.wavebeans.lib.stream.*
import io.wavebeans.lib.stream.fft.fft
import io.wavebeans.lib.stream.fft.inverseFft
import io.wavebeans.lib.stream.window.hamming
import io.wavebeans.lib.stream.window.window
import io.wavebeans.metrics.MetricService
import org.spekframework.spek2.Spek
import org.spekframework.spek2.lifecycle.CachingMode
import org.spekframework.spek2.style.specification.describe
import java.io.File
import kotlin.math.abs

object FlattenSpec : Spek({

    val outputFile by memoized(CachingMode.TEST) { File.createTempFile("tmp", ".wav") }
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

    describe("Using FFT to tune the signal and restoring back after the processing") {
        modes.forEach { (mode, locateFacilitators, evaluate) ->
            it("should perform in $mode mode") {
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
                val actual = (wave("file://${outputFile.absolutePath}")).asSequence(sampleRate).drop(1000).take(10000).toList()
                assertThat(actual).isContainedBy(expected) { a, b -> abs(a - b) < 1e-4 }
            }
        }
    }

    describe("Smoothing the signal") {
        modes.forEach { (mode, locateFacilitators, evaluate) ->
            it("should perform in $mode mode") {
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
    }
})