package mux.lib.io

import assertk.assertThat
import assertk.assertions.each
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import mux.lib.RectangleWindow
import mux.lib.stream
import mux.lib.stream.fft
import mux.lib.stream.trim
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import kotlin.streams.toList


class CsvFftStreamOutputSpec : Spek({
    describe("FFT of signal with sample rate 4 Hz to CSV") {
        val sampleRate = 4.0f
        val x = (1..4)
                .stream(sampleRate)
                .fft(2, RectangleWindow(4))
                .trim(1000)

        val expectedFrequencies = listOf(0.0, 1.0)
        val expectedTimes = listOf(0.0, 500.0)

        describe("Generating magnitude") {
            val file = File.createTempFile("test_", ".mux.tmp")
            CsvFftStreamOutput(file.toURI(), x, true).use {csvOutput ->
                csvOutput.writer(sampleRate).use {
                    it.write(1000)
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
                    ).isEqualTo(expectedTimes)
                }

            }
        }

        describe("Generating phase") {
            val file = File.createTempFile("test_", ".mux.tmp")
            CsvFftStreamOutput(file.toURI(), x, false).use {csvOutput ->
                csvOutput.writer(sampleRate).use {
                    it.write(1000)
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
                    ).isEqualTo(expectedTimes)
                }

            }
        }


    }
})