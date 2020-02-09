package io.wavebeans.lib.io.table

import assertk.assertThat
import assertk.assertions.isCloseTo
import assertk.assertions.isEmpty
import io.wavebeans.lib.Sample
import io.wavebeans.lib.eachIndexed
import io.wavebeans.lib.ms
import io.wavebeans.lib.seqStream
import io.wavebeans.lib.stream.trim
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
                    .timeRange(800.ms, 900.ms)
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
            val samples = TableRegistry.instance().byName<Sample>(tableName)
                    .last(10000.ms) // more than should be available
                    .asSequence(1.0f)
                    .toList()
            assertThat(samples).eachIndexed(25) { s, i ->
                s.isCloseTo(24 * 1e-10 + i * 1e-10, 1e-14)
            }
        }

        it("should return only 250ms of last 500 ms written which is in range [74,99]") {
            val samples = TableRegistry.instance().byName<Sample>(tableName)
                    .last(10000.ms) // more than should be available
                    .asSequence(1.0f)
                    .toList()
            assertThat(samples).eachIndexed(25) { s, i ->
                s.isCloseTo(74 * 1e-10 + i * 1e-10, 1e-14)
            }
        }
    }
})