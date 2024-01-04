package io.wavebeans.lib.stream.window

import assertk.all
import assertk.assertThat
import assertk.assertions.*
import io.wavebeans.lib.*
import io.wavebeans.lib.stream.rangeProjection
import io.wavebeans.tests.eachIndexed
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object SampleMergedWindowStreamSpec : Spek({
    describe("Fixed window of size=2") {
        val stream1 = (0..3).stream().window(2)

        describe("Another stream the same size") {
            val stream2 = (10..13).stream().window(2)

            it("should sum up corresponding elements") {
                val res = (stream1 + stream2).asSequence(1.0f).asGroupedInts().toList()
                assertThat(res).at(0).isListOf(10, 12)
                assertThat(res).at(1).isListOf(14, 16)
            }

            it("should subtract corresponding elements correctly: stream2 - stream1") {
                val res = (stream2 - stream1).asSequence(1.0f).asGroupedInts().toList()
                assertThat(res).at(0).isListOf(10, 10)
                assertThat(res).at(1).isListOf(10, 10)
            }

            it("should subtracted corresponding elements: stream1 - stream2") {
                val res = (stream1 - stream2).asSequence(1.0f).asGroupedInts().toList()
                assertThat(res).at(0).isEqualTo(listOf(-10, -10))
                assertThat(res).at(1).isEqualTo(listOf(-10, -10))
            }

            it("should multiply corresponding elements: stream1 * stream2") {
                val s1 = DoubleStream(listOf(0.1, 0.2, 0.3, 0.4)).window(2)
                val s2 = DoubleStream(listOf(0.5, 0.6, 0.7, 0.8)).window(2)
                val res = (s1 * s2).asSequence(1.0f).asGroupedDoubles().toList()
                assertThat(res).at(0).eachIndexed(2) { v, i ->
                    v.isCloseTo(
                        when (i) {
                            0 -> 0.05
                            1 -> 0.12
                            else -> throw UnsupportedOperationException()
                        },
                        1e-10
                    )
                }
                assertThat(res).at(1).eachIndexed(2) { v, i ->
                    v.isCloseTo(
                        when (i) {
                            0 -> 0.21
                            1 -> 0.32
                            else -> throw UnsupportedOperationException()
                        },
                        1e-10
                    )
                }
            }

            it("should divide corresponding elements: stream1 / stream2") {
                val s1 = DoubleStream(listOf(0.1, 0.2, 0.3, 0.4)).window(2)
                val s2 = DoubleStream(listOf(0.2, 0.4, 0.6, 0.8)).window(2)
                val res = (s1 / s2).asSequence(1.0f).asGroupedDoubles().toList()
                assertThat(res).at(0).eachIndexed(2) { v, i ->
                    v.isCloseTo(
                        when (i) {
                            0 -> 0.5
                            1 -> 0.5
                            else -> throw UnsupportedOperationException()
                        },
                        1e-10
                    )
                }
                assertThat(res).at(1).eachIndexed(2) { v, i ->
                    v.isCloseTo(
                        when (i) {
                            0 -> 0.5
                            1 -> 0.5
                            else -> throw UnsupportedOperationException()
                        },
                        1e-10
                    )
                }
            }
        }

        describe("Another stream the same size partial windows") {
            val stream2 = (10..12).stream().window(2)

            it("should sum up corresponding elements") {
                val res = (stream1 + stream2).asSequence(1.0f).asGroupedInts().toList()
                assertThat(res).at(0).isListOf(10, 12)
                assertThat(res).at(1).isListOf(14, 3)
            }

            it("should subtract corresponding elements correctly: stream2 - stream1") {
                val res = (stream2 - stream1).asSequence(1.0f).asGroupedInts().toList()
                assertThat(res).at(0).isListOf(10, 10)
                assertThat(res).at(1).isEqualTo(listOf(10, -3))
            }

            it("should subtracted corresponding elements: stream1 - stream2") {
                val res = (stream1 - stream2).asSequence(1.0f).asGroupedInts().toList()
                assertThat(res).at(0).isEqualTo(listOf(-10, -10))
                assertThat(res).at(1).isEqualTo(listOf(-10, 3))
            }
        }

        describe("Another stream the same size with zeros") {
            val stream2 = IntStream(4.repeat { 0 }).window(2)

            it("should subtract corresponding elements: stream2 - stream1") {
                val res = (stream2 - stream1).asSequence(1.0f).asGroupedInts().toList()
                assertThat(res).at(0).isEqualTo(listOf(-0, -1))
                assertThat(res).at(1).isEqualTo(listOf(-2, -3))
            }

            it("should subtract corresponding elements: stream1 - stream2") {
                val res = (stream1 - stream2).asSequence(1.0f).asGroupedInts().toList()
                assertThat(res).at(0).isListOf(0, 1)
                assertThat(res).at(1).isListOf(2, 3)
            }

            it("should multiply corresponding elements: stream1 * stream2") {
                val s1 = DoubleStream(listOf(0.1, 0.2, 0.3, 0.4)).window(2)
                val s2 = DoubleStream(listOf(0.0, 0.0, 0.0, 0.0)).window(2)
                val res = (s1 * s2).asSequence(1.0f).asGroupedDoubles().toList()
                assertThat(res).at(0).eachIndexed(2) { v, _ -> v.isCloseTo(0.0, 1e-10) }
                assertThat(res).at(1).eachIndexed(2) { v, _ -> v.isCloseTo(0.0, 1e-10) }
            }

            it("should divide corresponding elements: stream2 / stream1") {
                val s1 = DoubleStream(listOf(0.1, 0.2, 0.3, 0.4)).window(2)
                val s2 = DoubleStream(listOf(0.0, 0.0, 0.0, 0.0)).window(2)
                val res = (s2 / s1).asSequence(1.0f).asGroupedDoubles().toList()
                assertThat(res).at(0).eachIndexed(2) { v, _ -> v.isCloseTo(0.0, 1e-10) }
                assertThat(res).at(1).eachIndexed(2) { v, _ -> v.isCloseTo(0.0, 1e-10) }
            }

            it("should divide corresponding elements: stream1 / stream2") {
                val s1 = DoubleStream(listOf(0.1, 0.2, 0.3, 0.4)).window(2)
                val s2 = DoubleStream(listOf(0.0, 0.0, -0.0, -0.0)).window(2)
                val res = (s1 / s2).asSequence(1.0f).asGroupedDoubles().toList()
                assertThat(res).at(0).eachIndexed(2) { v, _ -> v.isCloseTo(Double.POSITIVE_INFINITY, 1e-10) }
                assertThat(res).at(1).eachIndexed(2) { v, _ -> v.isCloseTo(Double.NEGATIVE_INFINITY, 1e-10) }
            }

        }

        describe("Another stream the same size but longer") {
            val stream2 = (10..15).stream().window(2)

            it("should sum up corresponding elements") {
                val res = (stream1 + stream2).asSequence(1.0f).asGroupedInts().toList()
                assertThat(res).at(0).isListOf(10, 12)
                assertThat(res).at(1).isListOf(14, 16)
                assertThat(res).at(2).isListOf(14, 15)
            }

            it("should subtract corresponding elements: stream2 - stream1") {
                val res = (stream2 - stream1).asSequence(1.0f).asGroupedInts().toList()
                assertThat(res).at(0).isListOf(10, 10)
                assertThat(res).at(1).isListOf(10, 10)
                assertThat(res).at(2).isListOf(14, 15)
            }

            it("should subtract corresponding elements: stream1 - stream2") {
                val res = (stream1 - stream2).asSequence(1.0f).asGroupedInts().toList()
                assertThat(res).at(0).isEqualTo(listOf(-10, -10))
                assertThat(res).at(1).isEqualTo(listOf(-10, -10))
                assertThat(res).at(2).isEqualTo(listOf(-14, -15))
            }
        }

        describe("Another stream different size") {
            val stream2 = (10..15).stream().window(3)

            it("should not be summed up") {
                assertThat { (stream1 + stream2).asSequence(1.0f).asGroupedInts().toList() }
                    .isFailure()
                    .isNotNull()
                    .message()
                    .isEqualTo("Can't merge with stream with different window size or step")
            }

            it("should not be subtracted") {
                assertThat { (stream1 - stream2).asSequence(1.0f).asGroupedInts().toList() }
                    .isFailure()
                    .isNotNull()
                    .message()
                    .isEqualTo("Can't merge with stream with different window size or step")
            }

        }

        describe("getting sub-range of merged stream.") {
            /*
                 1>   0  1  2  3
                 2>   10 11 12 13
                 +>   10 12 14 16
                      ^--^           - window #0 0-999.99[9]ms
                            ^--^     - window #1 1000-1999.99[9]ms
             */

            val stream2 = (10..13).stream().window(2)
            val sampleRate = 2.0f

            it("should return valid range if one window touched") {
                val range = (stream1 + stream2).rangeProjection(0, 999).asSequence(sampleRate).asGroupedInts().toList()
                assertThat(range).size().isEqualTo(1)
                assertThat(range).at(0).isListOf(10, 12)
            }

            it("should return valid range if several windows touched") {
                val range = (stream1 + stream2).rangeProjection(0, 1001).asSequence(sampleRate).asGroupedInts().toList()
                assertThat(range).size().isEqualTo(2)
                assertThat(range).at(0).isListOf(10, 12)
                assertThat(range).at(1).isListOf(14, 16)
            }

        }
    }


    describe("Sliding window of size=3 and step=2") {
        val stream1 = (0..3).stream().window(3, 2)

        describe("Another stream the same size") {
            val stream2 = (10..13).stream().window(3, 2)

            it("should sum up corresponding elements") {
                val res = (stream1 + stream2).asSequence(1.0f).asGroupedInts().toList()
                assertThat(res).at(0).isListOf(10, 12, 14)
                assertThat(res).at(1).isListOf(14, 16, 0)
            }

            it("should subtract corresponding elements correctly: stream2 - stream1") {
                val res = (stream2 - stream1).asSequence(1.0f).asGroupedInts().toList()
                assertThat(res).at(0).isListOf(10, 10, 10)
                assertThat(res).at(1).isListOf(10, 10, 0)
            }

            it("should subtracted corresponding elements: stream1 - stream2") {
                val res = (stream1 - stream2).asSequence(1.0f).asGroupedInts().toList()
                assertThat(res).at(0).isEqualTo(listOf(-10, -10, -10))
                assertThat(res).at(1).isEqualTo(listOf(-10, -10, 0))
            }
        }

        describe("Another stream the same size partial windows") {
            val stream2 = (10..12).stream().window(3, 2)

            it("should sum up corresponding elements") {
                val res = (stream1 + stream2).asSequence(1.0f).asGroupedInts().toList()
                assertThat(res).at(0).isListOf(10, 12, 14)
                assertThat(res).at(1).isListOf(14, 3, 0)
            }

            it("should subtract corresponding elements correctly: stream2 - stream1") {
                val res = (stream2 - stream1).asSequence(1.0f).asGroupedInts().toList()
                assertThat(res).at(0).isListOf(10, 10, 10)
                assertThat(res).at(1).isListOf(10, -3, 0)
            }

            it("should subtracted corresponding elements: stream1 - stream2") {
                val res = (stream1 - stream2).asSequence(1.0f).asGroupedInts().toList()
                assertThat(res).at(0).isListOf(-10, -10, -10)
                assertThat(res).at(1).isListOf(-10, 3, 0)
            }
        }

        describe("Another stream the same size with zeros") {
            val stream2 = IntStream(4.repeat { 0 }).window(3, 2)

            it("should sum up corresponding elements") {
                val res = (stream2 + stream1).asSequence(1.0f).asGroupedInts().toList()
                assertThat(res).at(0).isListOf(0, 1, 2)
                assertThat(res).at(1).isListOf(2, 3, 0)
            }

            it("should subtract corresponding elements: stream2 - stream1") {
                val res = (stream2 - stream1).asSequence(1.0f).asGroupedInts().toList()
                assertThat(res).at(0).isListOf(-0, -1, -2)
                assertThat(res).at(1).isListOf(-2, -3, 0)
            }

            it("should subtract corresponding elements: stream1 - stream2") {
                val res = (stream1 - stream2).asSequence(1.0f).asGroupedInts().toList()
                assertThat(res).at(0).isListOf(0, 1, 2)
                assertThat(res).at(1).isListOf(2, 3, 0)
            }
        }

        describe("Another stream the same size but longer") {
            val stream2 = (10..15).stream().window(3, 2)

            it("should sum up corresponding elements") {
                val res = (stream1 + stream2).asSequence(1.0f).asGroupedInts().toList()
                assertThat(res).at(0).isListOf(10, 12, 14)
                assertThat(res).at(1).isListOf(14, 16, 14)
                assertThat(res).at(2).isListOf(14, 15, 0)
            }

            it("should subtract corresponding elements: stream2 - stream1") {
                val res = (stream2 - stream1).asSequence(1.0f).asGroupedInts().toList()
                assertThat(res).at(0).isListOf(10, 10, 10)
                assertThat(res).at(1).isListOf(10, 10, 14)
                assertThat(res).at(2).isListOf(14, 15, 0)
            }

            it("should subtract corresponding elements: stream1 - stream2") {
                val res = (stream1 - stream2).asSequence(1.0f).asGroupedInts().toList()
                assertThat(res).at(0).isListOf(-10, -10, -10)
                assertThat(res).at(1).isListOf(-10, -10, -14)
                assertThat(res).at(2).isListOf(-14, -15, 0)
            }
        }

        describe("Another stream different size") {
            val stream2 = (10..15).stream().window(4, 2)

            it("should not be summed up") {
                assertThat { (stream1 + stream2).asSequence(1.0f).asGroupedInts().toList() }
                    .isFailure()
                    .isNotNull()
                    .message()
                    .isEqualTo("Can't merge with stream with different window size or step")
            }

            it("should not be subtracted") {
                assertThat { (stream1 - stream2).asSequence(1.0f).asGroupedInts().toList() }
                    .isFailure()
                    .isNotNull()
                    .message()
                    .isEqualTo("Can't merge with stream with different window size or step")
            }

        }

        describe("getting sub-range of merged stream.") {
            /*
                 1>    0  1  2  3
                 2>   10 11 12 13
                 +>   10 12 14 16 0
                      ^------^           - window #0 0-1499.99[9]ms
                            ^------^     - window #1 1000-2499.99[9]ms
             */

            val stream2 = (10..13).stream().window(3, 2)
            val sampleRate = 2.0f

            it("should return valid range if one window touched") {
                val range = (stream1 + stream2).rangeProjection(0, 999).asSequence(sampleRate).asGroupedInts().toList()
                assertThat(range).size().isEqualTo(1)
                assertThat(range).at(0).isListOf(10, 12, 14)
            }

            it("should return valid range if several windows touched") {
                val range = (stream1 + stream2).rangeProjection(0, 1001).asSequence(sampleRate).asGroupedInts().toList()
                assertThat(range).size().isEqualTo(2)
                assertThat(range).at(0).isListOf(10, 12, 14)
                assertThat(range).at(1).isListOf(14, 16, 0)
            }

        }
    }

    describe("Complex operations") {
        describe("Fixed window. 3-2+1") {
            /*
              1>      0   1   2   3
              2>     10  11  12  13
              3>     20  21  22
              3-2>   10  10  10 -13
              3-2+1> 10  11  12 -10
                     ^---^            - window #0 0-999.99[9]ms
                             ^---^    - window #1 1000-1999.99[9]ms
            */

            val s1 = (0..3).stream().window(2)
            val s2 = (10..13).stream().window(2)
            val s3 = (20..22).stream().window(2)

            val res = s3 - s2 + s1
            it("should calculate the sequence") {
                assertThat(res.asSequence(1.0f).asGroupedInts().toList()).all {
                    at(0).isListOf(10, 11)
                    at(1).isListOf(12, -10)
                }
            }
            it("should get range") {
                assertThat(res.rangeProjection(1000, null).asSequence(2.0f).asGroupedInts().toList()).all {
                    at(0).isListOf(12, -10)
                }
            }
        }

        describe("Sliding window. 3-2+1") {
            /*
              1>      0   1   2   3
              2>     10  11  12  13
              3>     20  21  22
              3-2>   10  10  10 -13  0
              3-2+1> 10  11  12 -10  0
                     ^--------^          - window #0 0-1499.99[9]ms
                             ^-------^   - window #1 1000-2499.99[9]ms
            */

            val s1 = (0..3).stream().window(3, 2)
            val s2 = (10..13).stream().window(3, 2)
            val s3 = (20..22).stream().window(3, 2)

            val res = s3 - s2 + s1
            it("should calculate the sequence") {
                assertThat(res.asSequence(1.0f).asGroupedInts().toList()).all {
                    at(0).isListOf(10, 11, 12)
                    at(1).isListOf(12, -10, 0)
                }
            }
            it("should get range") {
                assertThat(res.rangeProjection(999, null).asSequence(2.0f).asGroupedInts().toList()).all {
                    at(0).isListOf(12, -10, 0)
                }
            }
        }
    }
})