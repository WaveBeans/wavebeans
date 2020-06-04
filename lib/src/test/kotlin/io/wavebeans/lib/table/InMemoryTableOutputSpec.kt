package io.wavebeans.lib.table

import assertk.Assert
import assertk.all
import assertk.assertThat
import assertk.assertions.*
import io.wavebeans.lib.*
import io.wavebeans.lib.io.Writer
import io.wavebeans.lib.stream.fft.FftSample
import io.wavebeans.lib.stream.fft.fft
import io.wavebeans.lib.stream.trim
import io.wavebeans.lib.stream.window.Window
import io.wavebeans.lib.stream.window.window
import mu.KotlinLogging
import org.spekframework.spek2.Spek
import org.spekframework.spek2.lifecycle.CachingMode.*
import org.spekframework.spek2.style.specification.describe
import java.lang.Thread.sleep
import java.util.concurrent.*
import java.util.concurrent.TimeUnit.*
import java.util.concurrent.atomic.AtomicReference

object InMemoryTableOutputSpec : Spek({

    describe("Operations on closed table") {
        val tableName = "table1"

        beforeGroup {
            seqStream()
                    .trim(1000)
                    .toTable(tableName)
                    .also {
                        it.writer(1000.0f).use { writer ->
                            writer.writeSome(1000)
                        }
                    }
        }

        val table by memoized(SCOPE) { TableRegistry.default.byName<Sample>(tableName) }

        it("should return last 100ms") {
            val samples = table
                    .last(100.ms)
                    .asSequence(1.0f)
                    .toList()
            assertThat(samples).eachIndexed(100) { s, i ->
                s.isCloseTo(899 * 1e-10 + i * 1e-10, 1e-14)
            }
        }

        it("should return pre-last 100ms") {
            val samples = table
                    .timeRange(800.ms, 0.9e9.ns)
                    .asSequence(1.0f)
                    .toList()
            assertThat(samples).eachIndexed(100) { s, i ->
                s.isCloseTo(800 * 1e-10 + i * 1e-10, 1e-14)
            }
        }

        it("should return nothing for out of range request") {
            val samples = table
                    .timeRange(1000.ms, 1100.ms)
                    .asSequence(1.0f)
                    .toList()
            assertThat(samples).isEmpty()
        }
    }

    describe("Operations between table filling in") {
        val tableName = "table2"

        val output by memoized(SCOPE) { seqStream().toTable(tableName) }
        val writer by memoized(SCOPE) { output.writer(1000.0f) }
        val table by memoized { TableRegistry.default.byName<Sample>(tableName) }

        beforeEachTest {
            // read a new chunk before each test
            writer.writeSome(500)
        }

        afterGroup {
            writer.close()
        }

        it("should return samples range [399,499]") {
            val samples = table
                    .last(100.ms)
                    .asSequence(1.0f)
                    .toList()
            assertThat(samples).eachIndexed(100) { s, i ->
                s.isCloseTo(399 * 1e-10 + i * 1e-10, 1e-14)
            }
        }


        it("should return sample range [899,999]") {
            val samples = table
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

            val output by memoized(SCOPE) {
                seqStream()
                        .trim(100)
                        .toTable(tableName, 25.ms)
            }
            val writer by memoized(SCOPE) { output.writer(1000.0f) }
            val table by memoized(SCOPE) { TableRegistry.default.byName<Sample>(tableName) as InMemoryTimeseriesTableDriver }

            beforeEachTest {
                // read a new chunk before each test
                writer.writeSome(50)
                table.performCleanup()
            }

            afterGroup {
                writer.close()
            }

            it("should return only 250ms of first 500 ms written which is in range [24,49]") {
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
                    writer.writeSome(5)
                    (TableRegistry.default.byName<Sample>(tableName) as InMemoryTimeseriesTableDriver).performCleanup()
                }

                afterGroup {
                    writer.close()
                }

                it("should return only 250ms of first 500 ms written") {
                    val table = TableRegistry.default.byName<Window<Sample>>(tableName)

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
                    val table = TableRegistry.default.byName<Window<Sample>>(tableName)

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
                    writer.writeSome(5)
                    (TableRegistry.default.byName<Sample>(tableName) as InMemoryTimeseriesTableDriver).performCleanup()
                }

                afterGroup {
                    writer.close()
                }

                it("should return only 250ms of first 500 ms written") {
                    val table = TableRegistry.default.byName<FftSample>(tableName)

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
                    val table = TableRegistry.default.byName<FftSample>(tableName)

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

    describe("Streaming data from table") {

        describe("No initial offset") {
            val tableName = "tableStream1"

            val writer by memoized(SCOPE) { seqStream().toTable(tableName, 25.ms).writer(1000.0f) }
            val table by memoized(SCOPE) {
                TableRegistry.default.byName<Sample>(tableName) as InMemoryTimeseriesTableDriver
            }
            val iterator by memoized(SCOPE) {
                (table.stream(0.s).asSequence(1000.0f).iterator() as ContinuousReadTableIterator<Sample>)
                        .also { it.perElementLog = true }
            }

            beforeGroup {
                writer.writeSome(0) // initialize writer
            }

            it("should prepare the iterator") {
                assertThat(iterator).all {
                    hasNext().isTrue()
                    returned().isEqualTo(0L)
                }
            }
            it("should not read any elements within 100 ms") {
                assertThat(iterator).all {
                    tryTake(10, timeout = 100).isEmpty()
                    returned().isEqualTo(0L)
                }
            }

            it("should perform clean up and remove 0 elements") { assertThat(table.performCleanup()).isEqualTo(0) }
            it("should read first 25 elements") {
                writer.writeSome(25)
                assertThat(iterator).all {
                    take(25).eachIndexed(25) { s, i ->
                        s.isCloseTo(0 * 1e-10 + i * 1e-10, 1e-14)
                    }
                    returned().isEqualTo(25L)
                }
            }

            it("should perform clean up and remove 0 elements") { assertThat(table.performCleanup()).isEqualTo(0) }
            it("should read second 25 elements") {
                writer.writeSome(25)
                assertThat(iterator).all {
                    take(25).eachIndexed(25) { s, i ->
                        s.isCloseTo(25 * 1e-10 + i * 1e-10, 1e-14)
                    }
                    returned().isEqualTo(50L)
                }
            }

            it("should perform clean up and remove 24 elements") { assertThat(table.performCleanup()).isEqualTo(24) }
            it("should read third 25 elements") {
                writer.writeSome(25)
                assertThat(iterator).all {
                    take(25).eachIndexed(25) { s, i ->
                        s.isCloseTo(50 * 1e-10 + i * 1e-10, 1e-14)
                    }
                    returned().isEqualTo(75L)
                }
            }

            it("should perform clean up and remove 25 elements") { assertThat(table.performCleanup()).isEqualTo(25) }
            it("should not read any elements within 100 ms") {
                assertThat(iterator).all {
                    tryTake(10, timeout = 100).isEmpty()
                    returned().isEqualTo(75L)
                }
            }
            it("should read only 5 elements within 100 ms") {
                writer.writeSome(5)
                assertThat(iterator).all {
                    tryTake(10, timeout = 100).eachIndexed(5) { s, i ->
                        s.isCloseTo(75 * 1e-10 + i * 1e-10, 1e-14)
                    }
                    returned().isEqualTo(80L)
                }
            }
        }

        describe("With some intial offset") {
            val tableName = "tableStream2"
            val writer by memoized(SCOPE) {
                seqStream().toTable(tableName, 25.ms).writer(1000.0f)
                        // generate some initial data
                        .also { w -> repeat(10) { if (!w.write()) throw IllegalStateException() } }
            }
            val table by memoized(SCOPE) {
                TableRegistry.default.byName<Sample>(tableName) as InMemoryTimeseriesTableDriver
            }
            val iterator by memoized(SCOPE) {
                (table.stream(5.ms).asSequence(1000.0f).iterator() as ContinuousReadTableIterator<Sample>)
                        .also { it.perElementLog = true }
            }

            beforeGroup {
                writer.writeSome(0) // initialize writer
            }

            it("should read pregenerated 6 elements within 100 ms") {
                assertThat(iterator).all {
                    tryTake(20, timeout = 100).eachIndexed(6) { s, i ->
                        s.isCloseTo(4 * 1e-10 + i * 1e-10, 1e-14)
                    }
                    returned().isEqualTo(6L)
                }
            }
            it("should read another 25 elements when they are already written") {
                writer.writeSome(25)
                assertThat(iterator).all {
                    take(25).eachIndexed(25) { s, i ->
                        s.isCloseTo(10 * 1e-10 + i * 1e-10, 1e-14)
                    }
                    returned().isEqualTo(31L)
                }
            }
        }
    }
})

internal fun Writer.writeSome(count: Int) {
    repeat(count) { if (!this.write()) throw IllegalStateException("Can't write with $this") }
}

internal fun <T : Any> Assert<ContinuousReadTableIterator<T>>.take(count: Int): Assert<List<T>> = this.prop("take($count)") { it.take(count) }
internal fun <T : Any> Assert<ContinuousReadTableIterator<T>>.returned(): Assert<Long> = this.prop("returned") { it.returned }
internal fun <T : Any> Assert<ContinuousReadTableIterator<T>>.hasNext(): Assert<Boolean> = this.prop("hasNext") { it.hasNext() }

internal fun <T : Any> Assert<ContinuousReadTableIterator<T>>.tryTake(count: Int, timeout: Long): Assert<List<T>> {
    val log = KotlinLogging.logger { }
    return this.prop("tryTake($count, timeout=$timeout)") { iterator ->
        val l = mutableListOf<T>()
        val started = CountDownLatch(1)
        val stopped = CountDownLatch(1)
        val exception = AtomicReference<Exception>()
        val t = Thread {
            started.countDown()
            try {
                repeat(count) { l += iterator.next() }
            } catch (e: Exception) {
                if (e !is InterruptedException) exception.set(e)
                stopped.countDown()
            }
        }
        t.start()
        started.await(5000, MILLISECONDS)
        sleep(timeout)
        log.debug { "Trying take $count elements for $timeout ms. Only retrieved: $l" }
        t.interrupt()
        stopped.await(5000, MILLISECONDS)
        exception.get()?.let { throw it }
        l // return what we could have read
    }
}

internal fun <T> Iterator<T>.take(count: Int): List<T> = (0 until count).map { this.next() }
