package io.wavebeans.metrics.collector

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.isTrue
import assertk.fail
import io.wavebeans.metrics.eachIndexed
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe


object TimeseriesListSpec : Spek({
    describe("Int timeseries list with default 60 sec granular") {
        val list = TimeseriesList<Int> { a, b -> a + b }

        beforeEachTest { list.reset() }

        it("should return sum of added values inside the range. All values") {
            assertThat(list.append(1, 1.min)).isTrue()
            assertThat(list.append(2, 2.min)).isTrue()
            assertThat(list.append(3, 3.min)).isTrue()
            assertThat(list.inLast(5.min, 4.min)).isEqualTo(6)
        }
        it("should return sum of added values inside the range. Not all values") {
            assertThat(list.append(1, 1.min)).isTrue()
            assertThat(list.append(2, 2.min)).isTrue()
            assertThat(list.append(3, 3.min)).isTrue()
            assertThat(list.inLast(2.min, 4.min)).isEqualTo(5)
        }
        it("should clean up some values") {
            assertThat(list.append(1, 1.min)).isTrue()
            assertThat(list.append(2, 2.min)).isTrue()
            assertThat(list.append(3, 3.min)).isTrue()
            list.leaveOnlyLast(2.min, 3.5.min)
            assertThat(list.inLast(5.min, 4.min)).isEqualTo(5)
        }
        it("should clean up values in non-committed granular") {
            assertThat(list.append(1, 1.sec)).isTrue()
            assertThat(list.append(2, 2.sec)).isTrue()
            assertThat(list.append(3, 3.sec)).isTrue()
            list.leaveOnlyLast(1.min, 1.5.min)
            assertThat(list.inLast(5.min, 2.min)).isNull()
        }
        it("should remove all before and return it") {
            assertThat(list.append(1, 10.sec)).isTrue()
            assertThat(list.append(2, 20.sec)).isTrue()
            assertThat(list.append(3, 70.sec)).isTrue()

            assertThat(list.removeAllBefore(50.sec)).eachIndexed(1) { pair, index ->
                when (index) {
                    0 -> pair.isEqualTo(3 at 0.sec)
                    else -> fail("Index $index is not expected")
                }
            }
            assertThat(list.inLast(5.min, 2.min)).isEqualTo(3)
        }
        it("should remove all before and return it -- before is strict") {
            assertThat(list.append(1, 10.sec)).isTrue()
            assertThat(list.append(2, 20.sec)).isTrue()
            assertThat(list.append(4, 61.sec)).isTrue()
            assertThat(list.append(5, 70.sec)).isTrue()

            assertThat(list.removeAllBefore(61.sec)).eachIndexed(1) { pair, index ->
                when (index) {
                    0 -> pair.isEqualTo(3 at 0.sec)
                    else -> fail("Index $index is not expected")
                }
            }
            assertThat(list.inLast(5.min, 2.min)).isEqualTo(9)
        }
        it("should remove all before and return it -- non-committed only") {
            assertThat(list.append(1, 10.sec)).isTrue()
            assertThat(list.append(2, 20.sec)).isTrue()
            assertThat(list.append(3, 30.sec)).isTrue()
            assertThat(list.append(4, 40.sec)).isTrue()

            assertThat(list.removeAllBefore(61.sec)).eachIndexed(1) { pair, index ->
                when (index) {
                    0 -> pair.isEqualTo(10 at 40.sec)
                    else -> fail("Index $index is not expected")
                }
            }
            assertThat(list.inLast(5.min, 2.min)).isNull()
        }
        it("should merge with provided non-empty one") {
            assertThat(list.append(1, 1.min)).isTrue()
            assertThat(list.append(2, 1.5.min)).isTrue()
            assertThat(list.append(3, 3.min)).isTrue()

            list.merge(sequenceOf(
                    15 at 1.2.min,
                    25 at 2.5.min,
                    35 at 3.5.min,
                    4 at 4.min
            ))

            assertThat(list.removeAllBefore(Long.MAX_VALUE)).eachIndexed(4) { pair, index ->
                when (index) {
                    0 -> pair.isEqualTo(18 at 1.min)
                    1 -> pair.isEqualTo(25 at 2.min)
                    2 -> pair.isEqualTo(38 at 3.min)
                    3 -> pair.isEqualTo(4 at 4.min)
                    else -> fail("Index $index is not expected")
                }
            }
        }
    }

    describe("Int timeseries list without rolling up") {
        val list = TimeseriesList<Int>(granularValueInMs = 0) { a, b -> a + b }

        beforeEachTest { list.reset() }

        it("should return sum of added values inside the range. All values") {
            assertThat(list.append(1, 1.min)).isTrue()
            assertThat(list.append(2, 2.min)).isTrue()
            assertThat(list.append(3, 3.min)).isTrue()
            assertThat(list.inLast(5.min, 4.min)).isEqualTo(6)
        }
        it("should return sum of added values inside the range. Not all values") {
            assertThat(list.append(1, 1.min)).isTrue()
            assertThat(list.append(2, 2.min)).isTrue()
            assertThat(list.append(3, 3.min)).isTrue()
            assertThat(list.inLast(2.min, 4.min)).isEqualTo(5)
        }
        it("should clean up some values") {
            assertThat(list.append(1, 1.min)).isTrue()
            assertThat(list.append(2, 2.min)).isTrue()
            assertThat(list.append(3, 3.min)).isTrue()
            list.leaveOnlyLast(2.min, 3.5.min)
            assertThat(list.inLast(5.min, 4.min)).isEqualTo(5)
        }
        it("should clean up values in non-committed granular") {
            assertThat(list.append(1, 1.sec)).isTrue()
            assertThat(list.append(2, 2.sec)).isTrue()
            assertThat(list.append(3, 3.sec)).isTrue()
            list.leaveOnlyLast(1.min, 1.5.min)
            assertThat(list.inLast(5.min, 2.min)).isNull()
        }
        it("should remove all before and return it") {
            assertThat(list.append(1, 1.sec)).isTrue()
            assertThat(list.append(2, 2.sec)).isTrue()
            assertThat(list.append(3, 3.sec)).isTrue()

            assertThat(list.removeAllBefore(2.1.sec)).eachIndexed(2) { pair, index ->
                when (index) {
                    0 -> pair.isEqualTo(1 at 1.sec)
                    1 -> pair.isEqualTo(2 at 2.sec)
                    else -> fail("Index $index is not expected")
                }
            }
            assertThat(list.inLast(5.min, 2.min)).isEqualTo(3)
        }
        it("should remove all before and return it -- before is strict") {
            assertThat(list.append(1, 1.sec)).isTrue()
            assertThat(list.append(2, 2.sec)).isTrue()
            assertThat(list.append(3, 3.sec)).isTrue()

            assertThat(list.removeAllBefore(2.sec)).eachIndexed(1) { pair, index ->
                when (index) {
                    0 -> pair.isEqualTo(1 at 1.sec)
                    else -> fail("Index $index is not expected")
                }
            }
            assertThat(list.inLast(5.min, 2.min)).isEqualTo(5)
        }
        it("should merge with provided non-empty one") {
            assertThat(list.append(1, 1.sec)).isTrue()
            assertThat(list.append(2, 2.sec)).isTrue()
            assertThat(list.append(3, 3.sec)).isTrue()

            list.merge(sequenceOf(
                    15 at 1.5.sec,
                    25 at 2.5.sec,
                    35 at 3.5.sec,
                    4 at 4.sec
            ))

            assertThat(list.removeAllBefore(Long.MAX_VALUE)).eachIndexed(7) { timedValue, index ->
                when (index) {
                    0 -> timedValue.isEqualTo(1 at 1.sec)
                    1 -> timedValue.isEqualTo(15 at 1.5.sec)
                    2 -> timedValue.isEqualTo(2 at 2.sec)
                    3 -> timedValue.isEqualTo(25 at 2.5.sec)
                    4 -> timedValue.isEqualTo(3 at 3.sec)
                    5 -> timedValue.isEqualTo(35 at 3.5.sec)
                    6 -> timedValue.isEqualTo(4 at 4.sec)
                    else -> fail("Index $index is not expected")
                }
            }
        }
        it("should merge with provided non-empty one if it is empty") {
            list.merge(sequenceOf(
                    15 at 1.5.sec,
                    25 at 2.5.sec,
                    35 at 3.5.sec,
                    4 at 4.sec
            ))

            assertThat(list.removeAllBefore(Long.MAX_VALUE)).eachIndexed(4) { timedValue, index ->
                when (index) {
                    0 -> timedValue.isEqualTo(15 at 1.5.sec)
                    1 -> timedValue.isEqualTo(25 at 2.5.sec)
                    2 -> timedValue.isEqualTo(35 at 3.5.sec)
                    3 -> timedValue.isEqualTo(4 at 4.sec)
                    else -> fail("Index $index is not expected")
                }
            }
        }
        it("should merge with provided empty one if it is also empty") {
            list.merge(emptySequence())

            assertThat(list.removeAllBefore(Long.MAX_VALUE)).isEmpty()
        }
        it("should merge with provided empty one") {
            assertThat(list.append(1, 1.sec)).isTrue()
            assertThat(list.append(2, 2.sec)).isTrue()
            assertThat(list.append(3, 3.sec)).isTrue()

            list.merge(emptySequence())

            assertThat(list.removeAllBefore(Long.MAX_VALUE)).eachIndexed(3) { timedValue, index ->
                when (index) {
                    0 -> timedValue.isEqualTo(1 at 1.sec)
                    1 -> timedValue.isEqualTo(2 at 2.sec)
                    2 -> timedValue.isEqualTo(3 at 3.sec)
                    else -> fail("Index $index is not expected")
                }
            }
        }
    }

    describe("GaugeAccumulator without rolling up") {
        val list = TimeseriesList<GaugeAccumulator>(0) { a, b -> a + b }

        beforeEachTest { list.reset() }

        it("should accumulate all values") {
            assertThat(list.append(GaugeAccumulator(false, 1.0), 1.min)).isTrue()
            assertThat(list.append(GaugeAccumulator(false, 2.0), 2.min)).isTrue()
            assertThat(list.append(GaugeAccumulator(false, 3.0), 3.min)).isTrue()
            assertThat(list.inLast(5.min, 4.min)).isEqualTo(GaugeAccumulator(false, 6.0))
        }
        it("should accumulate values only after set and mark result as set") {
            assertThat(list.append(GaugeAccumulator(false, 1.0), 1.min)).isTrue()
            assertThat(list.append(GaugeAccumulator(true, 2.0), 2.min)).isTrue()
            assertThat(list.append(GaugeAccumulator(false, 3.0), 3.min)).isTrue()
            assertThat(list.inLast(5.min, 4.min)).isEqualTo(GaugeAccumulator(true, 5.0))
        }
        it("should accumulate values only after set and not mark result as set as set itself is out of range") {
            assertThat(list.append(GaugeAccumulator(true, 1.0), 1.min)).isTrue()
            assertThat(list.append(GaugeAccumulator(false, 2.0), 2.min)).isTrue()
            assertThat(list.append(GaugeAccumulator(false, 3.0), 3.min)).isTrue()
            assertThat(list.inLast(2.min, 4.min)).isEqualTo(GaugeAccumulator(false, 5.0))
        }
        it("should accumulate only set value") {
            assertThat(list.append(GaugeAccumulator(false, 1.0), 1.min)).isTrue()
            assertThat(list.append(GaugeAccumulator(false, 2.0), 2.min)).isTrue()
            assertThat(list.append(GaugeAccumulator(true, 3.0), 3.min)).isTrue()
            assertThat(list.inLast(5.min, 4.min)).isEqualTo(GaugeAccumulator(true, 3.0))
        }
    }

    describe("GaugeAccumulator with 60 sec granularity") {
        val list = TimeseriesList<GaugeAccumulator>(60000) { a, b -> a + b }

        beforeEachTest { list.reset() }

        it("should accumulate values") {
            assertThat(list.append(GaugeAccumulator(false, 1.0), 1.min)).isTrue()
            assertThat(list.append(GaugeAccumulator(false, 2.0), 1.5.min)).isTrue()
            assertThat(list.append(GaugeAccumulator(false, 3.0), 2.min)).isTrue()
            assertThat(list.append(GaugeAccumulator(false, 4.0), 2.5.min)).isTrue()
            assertThat(list.inLast(5.min, 4.min)).isEqualTo(GaugeAccumulator(false, 10.0))
        }
        it("should accumulate values only after set") {
            assertThat(list.append(GaugeAccumulator(false, 0.5), 0.5.min)).isTrue()
            assertThat(list.append(GaugeAccumulator(false, 1.0), 1.min)).isTrue()
            assertThat(list.append(GaugeAccumulator(true, 2.0), 1.5.min)).isTrue()
            assertThat(list.append(GaugeAccumulator(false, 3.0), 1.7.min)).isTrue()
            assertThat(list.append(GaugeAccumulator(false, 4.0), 2.min)).isTrue()
            assertThat(list.append(GaugeAccumulator(false, 5.0), 2.5.min)).isTrue()
            assertThat(list.inLast(5.min, 4.min)).isEqualTo(GaugeAccumulator(true, 14.0))
        }
        it("should accumulate only set value") {
            assertThat(list.append(GaugeAccumulator(false, 1.0), 1.min)).isTrue()
            assertThat(list.append(GaugeAccumulator(false, 2.0), 1.5.min)).isTrue()
            assertThat(list.append(GaugeAccumulator(false, 3.0), 2.min)).isTrue()
            assertThat(list.append(GaugeAccumulator(true, 4.0), 2.5.min)).isTrue()
            assertThat(list.inLast(5.min, 4.min)).isEqualTo(GaugeAccumulator(true, 4.0))
        }
    }
})

private val Number.sec: Long
    get() = (1000 * this.toDouble()).toLong()

private val Number.min: Long
    get() = (this.toDouble() * 60).sec

