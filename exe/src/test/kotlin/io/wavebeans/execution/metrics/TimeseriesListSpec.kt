package io.wavebeans.execution.metrics

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.isTrue
import assertk.fail
import io.wavebeans.execution.eachIndexed
import io.wavebeans.lib.m
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe


private val Number.sec: Long
    get() = (1000 * this.toDouble()).toLong()

private val Number.min: Long
    get() = (this.toDouble() * 60).sec

object TimeseriesListSpec : Spek({
    describe("Int timeseries list with default 60 sec granular") {
        val list = TimeseriesList<Int> { a, b -> a + b }

        beforeEachTest { list.reset() }

        it("should return sum of added values inside the range. All values") {
            assertThat(list.append(1, 1.min)).isTrue()
            assertThat(list.append(2, 2.min)).isTrue()
            assertThat(list.append(3, 3.min)).isTrue()
            assertThat(list.inLast(5.m, 4.min)).isEqualTo(6)
        }
        it("should return sum of added values inside the range. Not all values") {
            assertThat(list.append(1, 1.min)).isTrue()
            assertThat(list.append(2, 2.min)).isTrue()
            assertThat(list.append(3, 3.min)).isTrue()
            assertThat(list.inLast(2.m, 4.min)).isEqualTo(5)
        }
        it("should clean up some values") {
            assertThat(list.append(1, 1.min)).isTrue()
            assertThat(list.append(2, 2.min)).isTrue()
            assertThat(list.append(3, 3.min)).isTrue()
            list.leaveOnlyLast(2.m, 3.5.min)
            assertThat(list.inLast(5.m, 4.min)).isEqualTo(5)
        }
        it("should clean up values in non-committed granular") {
            assertThat(list.append(1, 1.sec)).isTrue()
            assertThat(list.append(2, 2.sec)).isTrue()
            assertThat(list.append(3, 3.sec)).isTrue()
            list.leaveOnlyLast(1.m, 1.5.min)
            assertThat(list.inLast(5.m, 2.min)).isNull()
        }
        it("should merge with provided non-empty one") {
            assertThat(list.append(1, 1.min)).isTrue()
            assertThat(list.append(2, 1.5.min)).isTrue()
            assertThat(list.append(3, 3.min)).isTrue()

            list.merge(listOf(
                    1.2.min to 15,
                    2.5.min to 25,
                    3.5.min to 35,
                    4.min to 4
            ))

            assertThat(list.removeAllBefore(Long.MAX_VALUE)).eachIndexed(4) { pair, index ->
                when(index) {
                    0 -> pair.isEqualTo(1.min to 18)
                    1 -> pair.isEqualTo(2.min to 25)
                    2 -> pair.isEqualTo(3.min to 38)
                    3 -> pair.isEqualTo(4.min to 4)
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
            assertThat(list.inLast(5.m, 4.min)).isEqualTo(6)
        }
        it("should return sum of added values inside the range. Not all values") {
            assertThat(list.append(1, 1.min)).isTrue()
            assertThat(list.append(2, 2.min)).isTrue()
            assertThat(list.append(3, 3.min)).isTrue()
            assertThat(list.inLast(2.m, 4.min)).isEqualTo(5)
        }
        it("should clean up some values") {
            assertThat(list.append(1, 1.min)).isTrue()
            assertThat(list.append(2, 2.min)).isTrue()
            assertThat(list.append(3, 3.min)).isTrue()
            list.leaveOnlyLast(2.m, 3.5.min)
            assertThat(list.inLast(5.m, 4.min)).isEqualTo(5)
        }
        it("should clean up values in non-committed granular") {
            assertThat(list.append(1, 1.sec)).isTrue()
            assertThat(list.append(2, 2.sec)).isTrue()
            assertThat(list.append(3, 3.sec)).isTrue()
            list.leaveOnlyLast(1.m, 1.5.min)
            assertThat(list.inLast(5.m, 2.min)).isNull()
        }
        it("should merge with provided non-empty one") {
            assertThat(list.append(1, 1.sec)).isTrue()
            assertThat(list.append(2, 2.sec)).isTrue()
            assertThat(list.append(3, 3.sec)).isTrue()

            list.merge(listOf(
                    1.5.sec to 15,
                    2.5.sec to 25,
                    3.5.sec to 35,
                    4.sec to 4
            ))

            assertThat(list.removeAllBefore(Long.MAX_VALUE)).eachIndexed(7) { pair, index ->
                when(index) {
                    0 -> pair.isEqualTo(1.sec to 1)
                    1 -> pair.isEqualTo(1.5.sec to 15)
                    2 -> pair.isEqualTo(2.sec to 2)
                    3 -> pair.isEqualTo(2.5.sec to 25)
                    4 -> pair.isEqualTo(3.sec to 3)
                    5 -> pair.isEqualTo(3.5.sec to 35)
                    6 -> pair.isEqualTo(4.sec to 4)
                    else -> fail("Index $index is not expected")
                }
            }
        }
        it("should merge with provided non-empty one if it is empty") {
            list.merge(listOf(
                    1.5.sec to 15,
                    2.5.sec to 25,
                    3.5.sec to 35,
                    4.sec to 4
            ))

            assertThat(list.removeAllBefore(Long.MAX_VALUE)).eachIndexed(4) { pair, index ->
                when(index) {
                    0 -> pair.isEqualTo(1.5.sec to 15)
                    1 -> pair.isEqualTo(2.5.sec to 25)
                    2 -> pair.isEqualTo(3.5.sec to 35)
                    3 -> pair.isEqualTo(4.sec to 4)
                    else -> fail("Index $index is not expected")
                }
            }
        }
        it("should merge with provided empty one if it is also empty") {
            list.merge(emptyList())

            assertThat(list.removeAllBefore(Long.MAX_VALUE)).isEmpty()
        }
        it("should merge with provided empty one") {
            assertThat(list.append(1, 1.sec)).isTrue()
            assertThat(list.append(2, 2.sec)).isTrue()
            assertThat(list.append(3, 3.sec)).isTrue()

            list.merge(emptyList())

            assertThat(list.removeAllBefore(Long.MAX_VALUE)).eachIndexed(3) { pair, index ->
                when(index) {
                    0 -> pair.isEqualTo(1.sec to 1)
                    1 -> pair.isEqualTo(2.sec to 2)
                    2 -> pair.isEqualTo(3.sec to 3)
                    else -> fail("Index $index is not expected")
                }
            }
        }
    }
})