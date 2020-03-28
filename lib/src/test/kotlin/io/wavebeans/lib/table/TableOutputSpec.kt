package io.wavebeans.lib.table

import assertk.all
import assertk.assertThat
import assertk.assertions.*
import io.wavebeans.lib.*
import io.wavebeans.lib.stream.fft.FftSample
import io.wavebeans.lib.stream.fft.fft
import io.wavebeans.lib.stream.trim
import io.wavebeans.lib.stream.window.Window
import io.wavebeans.lib.stream.window.window
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TableOutputSpec : Spek({
    describe("Operations on closed table") {
        val tableName = "table1"
        val output = seqStream()
                .trim(1000)
                .toTable(tableName)

        output.writer(1000.0f).use { writer ->
            while (writer.write()) {
            }
        }

        it("should return last 100ms") {
            val samples = TableRegistry.instance().byName<Sample>(tableName)
                    .last(100.ms)
                    .asSequence(1.0f)
                    .toList()
            assertThat(samples).eachIndexed(100) { s, i ->
                s.isCloseTo(899 * 1e-10 + i * 1e-10, 1e-14)
            }
        }

        it("should return pre-last 100ms") {
            val samples = TableRegistry.instance().byName<Sample>(tableName)
                    .timeRange(800.ms, 0.9e9.ns)
                    .asSequence(1.0f)
                    .toList()
            assertThat(samples).eachIndexed(100) { s, i ->
                s.isCloseTo(800 * 1e-10 + i * 1e-10, 1e-14)
            }
        }

        it("should return nothing for out of range request") {
            val samples = TableRegistry.instance().byName<Sample>(tableName)
                    .timeRange(1000.ms, 1100.ms)
                    .asSequence(1.0f)
                    .toList()
            assertThat(samples).isEmpty()
        }
    }

    describe("Operations between table filling in") {
        val tableName = "table2"
        val output = seqStream()
                .toTable(tableName)

        val writer = output.writer(1000.0f)

        beforeEachTest {
            // read a new chunk before each test
            repeat(500) { if (!writer.write()) throw IllegalStateException() }
        }

        after {
            writer.close()
        }

        it("should return samples range [399,499]") {
            val samples = TableRegistry.instance().byName<Sample>(tableName)
                    .last(100.ms)
                    .asSequence(1.0f)
                    .toList()
            assertThat(samples).eachIndexed(100) { s, i ->
                s.isCloseTo(399 * 1e-10 + i * 1e-10, 1e-14)
            }
        }


        it("should return sample range [899,999]") {
            val samples = TableRegistry.instance().byName<Sample>(tableName)
                    .last(100.ms)
                    .asSequence(1.0f)
                    .toList()
            assertThat(samples).eachIndexed(100) { s, i ->
                s.isCloseTo(899 * 1e-10 + i * 1e-10, 1e-14)
            }
        }
    }

    describe("Retaining only desired duration available") {
        describe("Simple object") {
            val tableName = "table3"
            val output = seqStream()
                    .trim(100)
                    .toTable(tableName, 25.ms)

            val writer = output.writer(1000.0f)

            beforeEachTest {
                // read a new chunk before each test
                repeat(50) { if (!writer.write()) throw IllegalStateException() }
                (TableRegistry.instance().byName<Sample>(tableName) as InMemoryTimeseriesTableDriver).performCleanup()
            }

            after {
                writer.close()
            }

            it("should return only 250ms of first 500 ms written which is in range [24,49]") {
                val table = TableRegistry.instance().byName<Sample>(tableName)

                assertThat(table.firstMarker()).isEqualTo(24.ms)
                assertThat(table.lastMarker()).isEqualTo(49.ms)

                val samples = table
                        .last(10000.ms) // more than should be available
                        .asSequence(1.0f)
                        .toList()
                assertThat(samples).eachIndexed(25) { s, i ->
                    s.isCloseTo(24 * 1e-10 + i * 1e-10, 1e-14)
                }
            }

            it("should return only 250ms of last 500 ms written which is in range [74,99]") {
                val table = TableRegistry.instance().byName<Sample>(tableName)

                assertThat(table.firstMarker()).isEqualTo(74.ms)
                assertThat(table.lastMarker()).isEqualTo(99.ms)

                val samples = table
                        .last(10000.ms) // more than should be available
                        .asSequence(1.0f)
                        .toList()
                assertThat(samples).eachIndexed(25) { s, i ->
                    s.isCloseTo(74 * 1e-10 + i * 1e-10, 1e-14)
                }
            }
        }
        describe("Complex object") {

            describe("Window<Sample>") {

                val tableName = "table3_complexObject_windowSample"
                val output = seqStream()
                        .window(10)
                        .trim(100)
                        .toTable(tableName, 25.ms)

                val writer = output.writer(1000.0f)

                beforeEachTest {
                    // read a new chunk before each test
                    repeat(5) { if (!writer.write()) throw IllegalStateException() }
                    (TableRegistry.instance().byName<Sample>(tableName) as InMemoryTimeseriesTableDriver).performCleanup()
                }

                after {
                    writer.close()
                }

                it("should return only 250ms of first 500 ms written") {
                    val table = TableRegistry.instance().byName<Window<Sample>>(tableName)

                    assertThat(table.firstMarker()).isEqualTo(20.ms)
                    assertThat(table.lastMarker()).isEqualTo(40.ms)

                    val samples = table
                            .last(10000.ms) // more than should be available
                            .asSequence(1.0f)
                            .toList()
                    assertThat(samples).eachIndexed(2) { w, i ->
                        w.prop("elements") { it.elements }.eachIndexed(10) { s, j ->
                            s.isCloseTo((20 + i * 10) * 1e-10 + j * 1e-10, 1e-14)
                        }
                    }
                }

                it("should return only 250ms of last 500 ms written") {
                    val table = TableRegistry.instance().byName<Window<Sample>>(tableName)

                    assertThat(table.firstMarker()).isEqualTo(70.ms)
                    assertThat(table.lastMarker()).isEqualTo(90.ms)

                    val samples = table
                            .last(10000.ms) // more than should be available
                            .asSequence(1.0f)
                            .toList()
                    assertThat(samples).eachIndexed(2) { w, i ->
                        w.prop("elements") { it.elements }.eachIndexed(10) { s, j ->
                            s.isCloseTo((70 + i * 10) * 1e-10 + j * 1e-10, 1e-14)
                        }
                    }
                }
            }

            describe("FftSample") {

                val tableName = "table3_complexObject_fftSample"
                val output = seqStream()
                        .window(10)
                        .fft(32)
                        .trim(100)
                        .toTable(tableName, 25.ms)

                val writer = output.writer(1000.0f)

                beforeEachTest {
                    // read a new chunk before each test
                    repeat(5) { if (!writer.write()) throw IllegalStateException() }
                    (TableRegistry.instance().byName<Sample>(tableName) as InMemoryTimeseriesTableDriver).performCleanup()
                }

                after {
                    writer.close()
                }

                it("should return only 250ms of first 500 ms written") {
                    val table = TableRegistry.instance().byName<FftSample>(tableName)

                    assertThat(table.firstMarker()).isEqualTo(20.ms)
                    assertThat(table.lastMarker()).isEqualTo(40.ms)

                    val samples = table
                            .last(10000.ms) // more than should be available
                            .asSequence(1.0f)
                            .toList()
                    assertThat(samples).all {
                        size().isEqualTo(2)
                        each { it.prop("fft") { it.fft }.size().isEqualTo(32) }
                    }
                }

                it("should return only 250ms of last 500 ms written") {
                    val table = TableRegistry.instance().byName<FftSample>(tableName)

                    assertThat(table.firstMarker()).isEqualTo(70.ms)
                    assertThat(table.lastMarker()).isEqualTo(90.ms)

                    val samples = table
                            .last(10000.ms) // more than should be available
                            .asSequence(1.0f)
                            .toList()
                    assertThat(samples).all {
                        size().isEqualTo(2)
                        each { it.prop("fft") { it.fft }.size().isEqualTo(32) }
                    }
                }
            }
        }
    }
})