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
})

private val Number.sec: Long
    get() = (1000 * this.toDouble()).toLong()

private val Number.min: Long
    get() = (this.toDouble() * 60).sec

