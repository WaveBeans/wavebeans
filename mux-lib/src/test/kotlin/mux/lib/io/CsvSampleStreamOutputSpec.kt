package mux.lib.io

import assertk.assertThat
import assertk.assertions.*
import mux.lib.RectangleWindow
import mux.lib.stream
import mux.lib.stream.fft
import mux.lib.stream.sine
import mux.lib.stream.trim
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import kotlin.streams.toList


class CsvSampleStreamOutputSpec : Spek({
    describe("A sinusoid of 10Hz, 500ms") {
        val sampleRate = 200.0f
        val x = 10.sine().trim(500)

        describe("Writing to CSV with 100ms steps") {
            val file = File.createTempFile("test_", ".mux.tmp")
            CsvSampleStreamOutput(file.toURI(), TimeUnit.MILLISECONDS, x).use { csvOutput ->
                csvOutput.writer(sampleRate).use {
                    while (it.write(100, TimeUnit.MILLISECONDS)) {
                    }
                }
            }

            val expectedTimes = (0 until 100) // overall 100 samples
                    .map { (it * 5).toDouble() } // 5 ms per sample

            BufferedReader(InputStreamReader(FileInputStream(file))).use { reader ->
                val lines = reader.lines().toList()

                it("should have 101 lines") { assertThat(lines.size).isEqualTo(101) }

                it("2+ lines, 2nd column should have double values of Signal") {
                    assertThat(
                            lines.drop(1)
                                    .map { l ->
                                        l.split(",")
                                                .drop(1)
                                                .map { it.toDoubleOrNull() }
                                    }
                    ).each { a ->
                        a.isNotEmpty()
                                .also { a.size().isEqualTo(1) }
                                .also { a.each { it.isNotNull() } }
                    }
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