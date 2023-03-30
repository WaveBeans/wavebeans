package io.wavebeans.lib.io

import assertk.all
import assertk.assertThat
import assertk.assertions.*
import io.wavebeans.fs.core.WbFileDriver
import io.wavebeans.lib.TimeUnit
import io.wavebeans.lib.stream.minus
import io.wavebeans.lib.stream.trim
import io.wavebeans.tests.eachIndexed
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.lang.Thread.sleep
import kotlin.math.absoluteValue


class CsvSampleStreamOutputSpec : Spek({

    beforeGroup {
        TestWbFileDriver.register()
        WbFileDriver.defaultLocalFileScheme = "test"
    }

    afterGroup {
        TestWbFileDriver.unregister()
    }

    describe("A sinusoid of 10Hz, 500ms") {
        val sampleRate = 200.0f
        val x = 10.sine().trim(500)

        it("should write to CSV with 100ms steps") {
            val fileUrl = "test:///${kotlin.random.Random.nextLong().absoluteValue.toString(36)}.tmp"
            x.toCsv(fileUrl, TimeUnit.MILLISECONDS).writer(sampleRate).use { w ->
                while (w.write()) {
                    sleep(0)
                }
            }

            val expectedTimes = (0 until 100) // overall 100 samples
                    .map { (it * 5).toDouble() } // 5 ms per sample

            val lines = TestWbFileDriver.driver.fs[fileUrl]?.decodeToString()?.trim()?.split("\n")

            assertThat(lines).isNotNull().all {
                size().isEqualTo(101)
                transform {
                    it.drop(1)
                            .map { l ->
                                l.split(",")
                                        .drop(1)
                                        .map { it.toDoubleOrNull() }
                            }
                }.eachIndexed(100) { a, _ ->
                    a.isNotEmpty()
                            .also { a.size().isEqualTo(1) }
                            .also { a.each { it.isNotNull() } }
                }
                transform {
                        it.drop(1)
                                .map { l ->
                                    l.split(",")
                                            .take(1)
                                            .map { it.toDouble() }
                                }
                                .flatten()
                                .sorted()
                }.isEqualTo(expectedTimes)
            }
        }
    }

    describe("A diff of 2 sinusoids of 10Hz and 20 Hz, 500ms") {
        val sampleRate = 200.0f
        val x = (10.sine() - 20.sine()).trim(500)

        it("should write to CSV with 100ms steps") {
            val fileUrl = "test:///${kotlin.random.Random.nextLong().absoluteValue.toString(36)}.tmp"
            x.toCsv(fileUrl, TimeUnit.MILLISECONDS).writer(sampleRate).use { w ->
                while (w.write()) {
                    sleep(0)
                }
            }

            val expectedTimes = (0 until 100) // overall 100 samples
                    .map { (it * 5).toDouble() } // 5 ms per sample


            val lines = TestWbFileDriver.driver.fs[fileUrl]?.decodeToString()?.trim()?.split("\n")
            assertThat(lines).isNotNull().all {
                size().isEqualTo(101)
                transform {
                    it.drop(1)
                            .map { l ->
                                l.split(",")
                                        .drop(1)
                                        .map { it.toDoubleOrNull() }
                            }
                }.each {
                    it.all {
                        isNotEmpty()
                        size().isEqualTo(1)
                        each { it.isNotNull() }
                    }
                }

                transform {
                    it.drop(1)
                            .map { l ->
                                l.split(",")
                                        .take(1)
                                        .map { it.toDouble() }
                            }
                            .flatten()
                            .sorted()
                }.isEqualTo(expectedTimes)
            }
        }
    }
})