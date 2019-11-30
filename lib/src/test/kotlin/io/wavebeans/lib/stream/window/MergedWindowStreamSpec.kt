package io.wavebeans.lib.stream.window

import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.message
import assertk.assertions.size
import assertk.catch
import io.wavebeans.lib.*
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object MergedWindowStreamSpec : Spek({
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
                val e = catch { (stream1 + stream2) }
                assertThat(e)
                        .isNotNull()
                        .message().isEqualTo("Can't merge with stream with different window size or step")
            }

            it("should not be subtracted") {
                val e = catch { (stream1 - stream2) }
                assertThat(e)
                        .isNotNull()
                        .message().isEqualTo("Can't merge with stream with different window size or step")
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
        val stream1 = (0..3).stream().window(3).sliding(2)

        describe("Another stream the same size") {
            val stream2 = (10..13).stream().window(3).sliding(2)

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
            val stream2 = (10..12).stream().window(3).sliding(2)

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
            val stream2 = IntStream(4.repeat { 0 }).window(3).sliding(2)

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
            val stream2 = (10..15).stream().window(3).sliding(2)

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
            val stream2 = (10..15).stream().window(4).sliding(2)

            it("should not be summed up") {
                val e = catch { (stream1 + stream2) }
                assertThat(e)
                        .isNotNull()
                        .message().isEqualTo("Can't merge with stream with different window size or step")
            }

            it("should not be subtracted") {
                val e = catch { (stream1 - stream2) }
                assertThat(e)
                        .isNotNull()
                        .message().isEqualTo("Can't merge with stream with different window size or step")
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

            val stream2 = (10..13).stream().window(3).sliding(2)
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

            val s1 = (0..3).stream().window(3).sliding(2)
            val s2 = (10..13).stream().window(3).sliding(2)
            val s3 = (20..22).stream().window(3).sliding(2)

            val res = s3 - s2 + s1
            it("should calculate the sequence") {
                assertThat(res.asSequence(1.0f).asGroupedInts().toList()).all {
                    at(0).isListOf(10, 11, 12)
                    at(1).isListOf(12, -10, 0)
                }
            }
            it("should get range") {
                assertThat(res.rangeProjection(1500, null).asSequence(2.0f).asGroupedInts().toList()).all {
                    at(0).isListOf(12, -10, 0)
                }
            }
        }
    }
})