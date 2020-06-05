package io.wavebeans.execution.distributed

import assertk.all
import assertk.assertThat
import assertk.assertions.*
import assertk.catch
import com.nhaarman.mockitokotlin2.*
import io.wavebeans.execution.eachIndexed
import io.wavebeans.execution.seqStream
import io.wavebeans.lib.Sample
import io.wavebeans.lib.s
import io.wavebeans.lib.sampleOf
import io.wavebeans.lib.table.*
import org.spekframework.spek2.Spek
import org.spekframework.spek2.lifecycle.CachingMode.SCOPE
import org.spekframework.spek2.lifecycle.CachingMode.TEST
import org.spekframework.spek2.style.specification.describe

object RemoteTimeseriesTableDriverSpec : Spek({
    describe("Pointing to Facilitator") {
        val tableName = "table1"
        val tableDriver by memoized(TEST) {
            mock<InMemoryTimeseriesTableDriver<Sample>>().also {
                val tableRegistry = TableRegistry.default
                if (tableRegistry.exists(tableName)) tableRegistry.unregister(tableName)
                tableRegistry.register(tableName, it)
            }
        }

        val facilitator by memoized(SCOPE) {
            Facilitator(
                    communicatorPort = 5000,
                    threadsNumber = 1,
                    onServerShutdownTimeoutMillis = 100
            )
        }

        beforeGroup {
            facilitator.start()
        }

        afterGroup {
            facilitator.terminate()
            facilitator.close()
        }


        val remoteTableDriver by memoized(SCOPE) { RemoteTimeseriesTableDriver<Sample>(tableName, "127.0.0.1:5000", Sample::class) }

        it("should init") {
            assertThat(catch { remoteTableDriver.init() }).isNull()
        }
        it("should return null for first marker") {
            whenever(tableDriver.firstMarker()).thenReturn(null)
            assertThat(remoteTableDriver.firstMarker()).isNull()
        }
        it("should return first marker value") {
            whenever(tableDriver.firstMarker()).thenReturn(1.s)
            assertThat(remoteTableDriver.firstMarker()).isNotNull().isEqualTo(1.s)
        }
        it("should return null for last marker") {
            whenever(tableDriver.lastMarker()).thenReturn(null)
            assertThat(remoteTableDriver.lastMarker()).isNull()
        }
        it("should return last marker value") {
            whenever(tableDriver.lastMarker()).thenReturn(100.s)
            assertThat(remoteTableDriver.lastMarker()).isNotNull().isEqualTo(100.s)
        }
        it("should put sample value") {
            tableDriver.init() // need to initialize memoized value
            remoteTableDriver.put(1.s, sampleOf(1.0))
            verify(tableDriver, times(1)).put(eq(1.s), eq(sampleOf(1.0)))
        }
        it("should reset") {
            tableDriver.init() // need to initialize memoized value
            remoteTableDriver.reset()
            verify(tableDriver, times(1)).reset()
        }

        describe("Different queries") {

            val queries = mapOf(
                    "last query" to LastIntervalTableQuery(1.s),
                    "time range query" to TimeRangeTableQuery(0.s, 1.s),
                    "continuous read query" to ContinuousReadTableQuery(0.s)
            )

            queries.forEach { (name, query) ->
                it("should make $name") {
                    whenever(tableDriver.tableType).thenReturn(Sample::class)
                    whenever(tableDriver.query(eq(query))).thenReturn(seqStream().asSequence(1.0f))
                    assertThat(remoteTableDriver.query(query))
                            .prop("take(5)") { it.take(5).toList() }.eachIndexed(5) { v, idx ->
                                v.isInstanceOf(Sample::class).isCloseTo(idx * 1e-10, 1e-14)
                            }
                }
            }
        }
    }
})