package io.wavebeans.http

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.wavebeans.lib.io.input
import io.wavebeans.lib.ms
import io.wavebeans.lib.stream.SampleCountMeasurement
import io.wavebeans.lib.table.TableRegistry
import io.wavebeans.lib.table.TimeseriesTableDriver
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TableServiceSpec : Spek({

    beforeGroup {
        SampleCountMeasurement.registerType(Int::class) { 1 }
    }

    describe("Existence") {
        val tableRegistry = mock<TableRegistry>()
        whenever(tableRegistry.exists(eq("table"))).thenReturn(true)

        val service = TableService(tableRegistry)

        it("should return true if the table exists") { assertThat(service.exists("table")).isTrue() }
        it("should return false if the table doesn't exist") { assertThat(service.exists("non-existing-table")).isFalse() }
    }

    describe("Last") {
        val tableRegistry = mock<TableRegistry>()
        val tableDriver = mock<TimeseriesTableDriver<Int>>()
        whenever(tableRegistry.exists(eq("table"))).thenReturn(true)
        whenever(tableRegistry.byName<Int>("table")).thenReturn(tableDriver)
        whenever(tableDriver.last(100.ms)).thenReturn(input { (i, sampleRate) -> if (i < sampleRate * 0.1) i.toInt() else null })

        val service = TableService(tableRegistry)

        it("should return stored values if table exists") {
            assertThat(service.last("table", 100.ms, 100.0f).bufferedReader().use { it.readLines() })
                    .isEqualTo((0..9).map { "{\"offset\":${it * 1_000_000_000L / 100L},\"value\":$it}" })
        }
        it("should not return any values if table doesn't exist") {
            assertThat(service.last("non-existing-table", 100.ms, 100.0f).bufferedReader().use { it.readLines() })
                    .isEmpty()
        }
    }

    describe("Time range") {
        val tableRegistry = mock<TableRegistry>()
        val tableDriver = mock<TimeseriesTableDriver<Int>>()
        whenever(tableRegistry.exists(eq("table"))).thenReturn(true)
        whenever(tableRegistry.byName<Int>("table")).thenReturn(tableDriver)
        whenever(tableDriver.timeRange(0.ms, 100.ms)).thenReturn(input { (i, sampleRate) -> if (i < sampleRate * 0.1) i.toInt() else null })

        val service = TableService(tableRegistry)

        it("should return stored values if table exists") {
            assertThat(service.timeRange("table", 0.ms, 100.ms, 100.0f).bufferedReader().use { it.readLines() })
                    .isEqualTo((0..9).map { "{\"offset\":${it * 1_000_000_000L / 100L},\"value\":$it}" })
        }
        it("should not return any values if table doesn't exist") {
            assertThat(service.timeRange("non-existing-table", 0.ms, 100.ms, 100.0f).bufferedReader().use { it.readLines() })
                    .isEmpty()
        }
    }
})