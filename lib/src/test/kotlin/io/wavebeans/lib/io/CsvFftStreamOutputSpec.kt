package io.wavebeans.lib.io

import assertk.assertThat
import assertk.assertions.each
import assertk.assertions.isCloseTo
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import io.wavebeans.lib.eachIndexed
import io.wavebeans.lib.stream
import io.wavebeans.lib.stream.fft.fft
import io.wavebeans.lib.stream.trim
import io.wavebeans.lib.stream.window.window
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.lang.Thread.sleep
import java.util.concurrent.TimeUnit
import kotlin.streams.toList


class CsvFftStreamOutputSpec : Spek({
    describe("FFT of signal with sample rate 4 Hz to CSV") {
        val sampleRate = 4.0f
        val x = (1..4)
                .stream(sampleRate)
                .trim(1000)
                .window(2)
                .fft(4)

        val expectedFrequencies = listOf(0.0, 1.0)
        val expectedTimes = listOf(0.0, 499.0)

        describe("Generating magnitude") {
            val file = File.createTempFile("test_", ".tmp")
            CsvFftStreamOutput(x, CsvFftStreamOutputParams("file://${file.absolutePath}", TimeUnit.MILLISECONDS, true)).writer(sampleRate).use { w ->
                while (w.write()) {
                    sleep(0)
                }
            }

            BufferedReader(InputStreamReader(FileInputStream(file))).use { reader ->
                val lines = reader.lines().toList()

                it("should have 3 lines") { assertThat(lines.size).isEqualTo(3) }

                it("Header should have frequencies in 2+ columns") {
                    assertThat(lines[0].split(",").drop(1).map { it.toDouble() }).isEqualTo(expectedFrequencies)
                }

                it("2+ lines, 2+ columns should have double values of FFT") {
                    assertThat(
                            lines.drop(1)
                                    .map { l ->
                                        l.split(",")
                                                .drop(1)
                                                .map { it.toDoubleOrNull() }
                                    }
                                    .flatten()
                    ).each { it.isNotNull() }
                }

                it("2+ lines, 1st column should have time in ms") {
                    assertThat(
                            lines.drop(1)
                                    .map { l ->
                                        l.split(",")
                                                .take(1)
                                                .map { it.toDouble() }
                                    }
                                    .flatten()
                                    .sorted()
                    ).eachIndexed(expectedTimes.size) {v, i->
                        v.isCloseTo(expectedTimes[i], 1e-6)
                    }
                }

            }
        }

        describe("Generating phase") {
            val file = File.createTempFile("test_", ".tmp")
            CsvFftStreamOutput(x, CsvFftStreamOutputParams("file://${file.absolutePath}", TimeUnit.MILLISECONDS, false)).writer(sampleRate).use { w ->
                while (w.write()) {
                    sleep(0)
                }
            }

            BufferedReader(InputStreamReader(FileInputStream(file))).use { reader ->
                val lines = reader.lines().toList()

                it("should have 3 lines") { assertThat(lines.size).isEqualTo(3) }

                it("Header should have frequencies in 2+ columns") {
                    assertThat(lines[0].split(",").drop(1).map { it.toDouble() }).isEqualTo(expectedFrequencies)
                }

                it("2+ lines, 2+ columns should have double values of FFT") {
                    assertThat(
                            lines.drop(1)
                                    .map { l ->
                                        l.split(",")
                                                .drop(1)
                                                .map { it.toDoubleOrNull() }
                                    }
                                    .flatten()
                    ).each { it.isNotNull() }
                }

                it("2+ lines, 1st column should have time in ms") {
                    assertThat(
                            lines.drop(1)
                                    .map { l ->
                                        l.split(",")
                                                .take(1)
                                                .map { it.toDouble() }
                                    }
                                    .flatten()
                                    .sorted()
                    ).eachIndexed(expectedTimes.size) {v, i->
                        v.isCloseTo(expectedTimes[i], 1e-6)
                    }
                }

            }
        }


    }
})