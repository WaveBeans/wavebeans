package io.wavebeans.lib.table

import io.wavebeans.lib.TimeMeasure
import io.wavebeans.lib.s
import io.wavebeans.lib.ms
import kotlin.test.*

class InMemoryTimeseriesTableDriverJsTest {

    @Test
    fun shouldPutAndQueryRange() {
        val driver = InMemoryTimeseriesTableDriver<String>(
            tableName = "test",
            tableType = String::class,
            retentionPolicy = object : TableRetentionPolicy {
                override fun isRetained(valueTimeMarker: TimeMeasure, maximumTimeMarker: TimeMeasure): Boolean = true
            },
            automaticCleanupEnabled = false
        )
        driver.init(1.0f)
        driver.put(0.s, "a")
        driver.put(1.s, "b")
        driver.put(2.s, "c")

        val result = driver.query(TimeRangeTableQuery(0.s, 2.s)).toList()
        assertEquals(listOf("a", "b"), result)

        val resultFull = driver.query(TimeRangeTableQuery(0.s, 3.s)).toList()
        assertEquals(listOf("a", "b", "c"), resultFull)
    }

    @Test
    fun shouldQueryLastInterval() {
        val driver = InMemoryTimeseriesTableDriver<String>(
            tableName = "test",
            tableType = String::class,
            retentionPolicy = object : TableRetentionPolicy {
                override fun isRetained(valueTimeMarker: TimeMeasure, maximumTimeMarker: TimeMeasure): Boolean = true
            },
            automaticCleanupEnabled = false
        )
        driver.init(1.0f)
        driver.put(0.s, "a")
        driver.put(1.s, "b")
        driver.put(2.s, "c")

        // to = 2.s, interval = 1.s -> from = 1.s. filter > 1.s -> only "c" at 2.s
        val result = driver.query(LastIntervalTableQuery(1.s)).toList()
        assertEquals(listOf("c"), result)

        // to = 2.s, interval = 1100.ms -> from = 0.9.s. filter > 0.9.s -> "b" at 1.s and "c" at 2.s
        val result2 = driver.query(LastIntervalTableQuery(1100.ms)).toList()
        assertEquals(listOf("b", "c"), result2)
    }

    @Test
    fun shouldCleanupAutomatically() {
        val driver = InMemoryTimeseriesTableDriver<String>(
            tableName = "test",
            tableType = String::class,
            retentionPolicy = TimeTableRetentionPolicy(1500.ms),
            automaticCleanupEnabled = true
        )
        driver.init(1.0f)
        driver.put(0.s, "a")
        driver.put(1.s, "b")
        driver.put(2.s, "c") // triggers cleanup, maximum is 2.s. 2.s - 1.5s = 0.5s. 0.s < 0.5s so 'a' is removed.

        assertEquals(2, driver.table.size)
        assertEquals("b", driver.firstMarker()?.let { driver.query(TimeRangeTableQuery(it, it + 1.ms)).firstOrNull() })
        assertEquals("c", driver.lastMarker()?.let { driver.query(TimeRangeTableQuery(it, it + 1.ms)).firstOrNull() })
    }

    @Test
    fun shouldHandleStreamFinish() {
        val driver = InMemoryTimeseriesTableDriver<String>(
            tableName = "test",
            tableType = String::class,
            retentionPolicy = object : TableRetentionPolicy {
                override fun isRetained(valueTimeMarker: TimeMeasure, maximumTimeMarker: TimeMeasure): Boolean = true
            },
            automaticCleanupEnabled = false
        )
        driver.init(1.0f)
        assertFalse(driver.isStreamFinished())
        driver.finishStream()
        assertTrue(driver.isStreamFinished())
        assertFailsWith<IllegalStateException> {
            driver.put(1.s, "a")
        }
    }

    @Test
    fun shouldFailIfPuttingOutOfOrder() {
        val driver = InMemoryTimeseriesTableDriver<String>(
            tableName = "test",
            tableType = String::class,
            retentionPolicy = object : TableRetentionPolicy {
                override fun isRetained(valueTimeMarker: TimeMeasure, maximumTimeMarker: TimeMeasure): Boolean = true
            },
            automaticCleanupEnabled = false
        )
        driver.init(1.0f)
        driver.put(1.s, "a")
        assertFailsWith<IllegalStateException> {
            driver.put(0.s, "b")
        }
    }
}
