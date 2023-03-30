package io.wavebeans.lib.io

import assertk.assertThat
import assertk.assertions.each
import assertk.assertions.isCloseTo
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import io.wavebeans.fs.core.WbFileDriver
import io.wavebeans.lib.TimeUnit
import io.wavebeans.lib.stream
import io.wavebeans.lib.stream.fft.fft
import io.wavebeans.lib.stream.trim
import io.wavebeans.lib.stream.window.window
import io.wavebeans.tests.eachIndexed
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.io.File
import java.lang.Thread.sleep


class CsvFftStreamOutputSpec : Spek({

    beforeGroup {
        TestWbFileDriver.register()
        WbFileDriver.defaultLocalFileScheme = "test"
    }

    afterGroup {
        TestWbFileDriver.unregister()
    }

    describe("FFT of signal with sample rate 4 Hz to CSV") {
        val sampleRate = 4.0f
        val x = (1..4)
            .stream(sampleRate)
            .trim(1000)
            .window(2)
            .fft(4)

        val expectedFrequencies = listOf(0.0, 1.0)
        val expectedTimes = listOf(0.0, 499.0)

        it("should generate magnitude") {
            val file = File.createTempFile("test_", ".tmp")
            CsvFftStreamOutput(
                x,
                CsvFftStreamOutputParams("test://${file.absolutePath}", TimeUnit.MILLISECONDS, true)
            ).writer(sampleRate).use { w ->
                while (w.write()) {
                    sleep(0)
                }
            }

            readFile(file).use { reader ->
                val lines = reader.lines().toList()

                assertThat(lines.size, "should have 3 lines").isEqualTo(3)

                assertThat(
                    lines[0].split(",").drop(1).map { it.toDouble() },
                    "Header should have frequencies in 2+ columns"
                )
                    .isEqualTo(expectedFrequencies)

                assertThat(
                    lines.drop(1)
                        .map { l ->
                            l.split(",")
                                .drop(1)
                                .map { it.toDoubleOrNull() }
                        }
                        .flatten(),
                    "2+ lines, 2+ columns should have double values of FFT"
                ).each { it.isNotNull() }

                assertThat(
                    lines.drop(1)
                        .map { l ->
                            l.split(",")
                                .take(1)
                                .map { it.toDouble() }
                        }
                        .flatten()
                        .sorted(),
                    "2+ lines, 1st column should have time in ms"
                ).eachIndexed(expectedTimes.size) { v, i ->
                    v.isCloseTo(expectedTimes[i], 1e-6)
                }
            }
        }

        it("should generate phase") {
            val file = File.createTempFile("test_", ".tmp")
            CsvFftStreamOutput(
                x,
                CsvFftStreamOutputParams("test://${file.absolutePath}", TimeUnit.MILLISECONDS, false)
            ).writer(sampleRate).use { w ->
                while (w.write()) {
                    sleep(0)
                }
            }

            readFile(file).use { reader ->
                val lines = reader.lines().toList()

                assertThat(lines.size, "should have 3 lines").isEqualTo(3)

                assertThat(
                    lines[0].split(",").drop(1).map { it.toDouble() },
                    "Header should have frequencies in 2+ columns"
                ).isEqualTo(expectedFrequencies)

                assertThat(
                    lines.drop(1)
                        .map { l ->
                            l.split(",")
                                .drop(1)
                                .map { it.toDoubleOrNull() }
                        }
                        .flatten(),
                    "2+ lines, 2+ columns should have double values of FFT"
                ).each { it.isNotNull() }

                assertThat(
                    lines.drop(1)
                        .map { l ->
                            l.split(",")
                                .take(1)
                                .map { it.toDouble() }
                        }
                        .flatten()
                        .sorted(),
                    "2+ lines, 1st column should have time in ms"
                ).eachIndexed(expectedTimes.size) { v, i ->
                    v.isCloseTo(expectedTimes[i], 1e-6)
                }

            }
        }
    }
})

private fun readFile(file: File) =
    ByteArrayInputStream(TestWbFileDriver.driver.fs.getValue("test://${file.absolutePath}"))
        .bufferedReader()