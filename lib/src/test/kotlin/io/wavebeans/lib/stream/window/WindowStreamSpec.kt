package io.wavebeans.lib.stream.window

import assertk.Assert
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.prop
import assertk.assertions.size
import io.wavebeans.lib.stream.IntStream
import io.wavebeans.lib.stream.asGroupedInts
import io.wavebeans.lib.stream.stream
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object WindowStreamSpec : Spek({

    describe("Fixed Window with size=2") {

        describe("No partial windows. ") {

            val windowedStream = (0..9).stream().window(2)

            it("should return values grouped by two") {
                val l = windowedStream.asSequence(1.0f).asGroupedInts().toList()
                assertThat(l).size().isEqualTo(5)
                assertThat(l[0]).isEqualTo(listOf(0, 1))
                assertThat(l[1]).isEqualTo(listOf(2, 3))
                assertThat(l[2]).isEqualTo(listOf(4, 5))
                assertThat(l[3]).isEqualTo(listOf(6, 7))
                assertThat(l[4]).isEqualTo(listOf(8, 9))
            }
        }

        describe("Partial windows. ") {

            val windowedStream = (0..8).stream().window(2)

            it("should return values grouped by two, the rest as a single list element") {
                val l = windowedStream.asSequence(1.0f).asGroupedInts().toList()
                assertThat(l).size().isEqualTo(5)
                assertThat(l[0]).isEqualTo(listOf(0, 1))
                assertThat(l[1]).isEqualTo(listOf(2, 3))
                assertThat(l[2]).isEqualTo(listOf(4, 5))
                assertThat(l[3]).isEqualTo(listOf(6, 7))
                assertThat(l[4]).isEqualTo(listOf(8))
            }
        }

        describe("No elements at all") {

            val windowedStream = IntStream(emptyList()).window(2)

            it("should return no values") {
                val l = windowedStream.asSequence(1.0f).asGroupedInts().toList()
                assertThat(l).size().isEqualTo(0)
            }
        }

        describe("Range projection. Sample rate 2Hz. Overall time 5 seconds") {

            /*
                 0 1 2 3 4 5 6 7 8 9
                 ^-^                 - window #0 with time 0-999.99[9]ms
                     ^-^             - window #1 with time 1000-1999.99[9]ms
                         ^-^         - window #2 with time 2000-2999.99[9]ms
                             ^-^     - window #3 with time 3000-3999.99[9]ms
                                 ^-^ - window #4 with time 4000-4999.99[9]ms
            */
            val windowStream = (0..9).stream().window(2)
            val sampleRate = 2.0f

            describe("Coinciding with Window.") {
                val range = windowStream.rangeProjection(0, 999)
                it("should return 1 window which it has intersection with") {
                    val l = range.asSequence(sampleRate).asGroupedInts().toList()
                    assertThat(l).size().isEqualTo(1)
                    assertThat(l).at(0).isEqualTo(listOf(0, 1))
                }
            }

            describe("Not-coinciding with Window.") {
                val range = windowStream.rangeProjection(499, 1421)
                it("should return 2 windows") {
                    val l = range.asSequence(sampleRate).asGroupedInts().toList()
                    assertThat(l).size().isEqualTo(2)
                    assertThat(l).at(0).isEqualTo(listOf(0, 1))
                    assertThat(l).at(1).isEqualTo(listOf(2, 3))
                }
            }

            describe("Out-of-range. Before") {
                val range = windowStream.rangeProjection(-42, 500)
                it("should return 1 window") {
                    val l = range.asSequence(sampleRate).asGroupedInts().toList()
                    assertThat(l).size().isEqualTo(1)
                    assertThat(l).at(0).isEqualTo(listOf(0, 1))
                }
            }

            describe("Out-of-range. After") {
                val range = windowStream.rangeProjection(4900, 500100)
                it("should return 1 window") {
                    val l = range.asSequence(sampleRate).asGroupedInts().toList()
                    assertThat(l).size().isEqualTo(1)
                    assertThat(l).at(0).isEqualTo(listOf(8, 9))
                }
            }
        }
    }

    describe("Sliding Window with size=3 and step=2") {

        describe("No partial windows.") {

            val windowedStream = (0..8).stream().window(3).sliding(2)

            it("should return values grouped by three, and one single-element list") {
                val l = windowedStream.asSequence(1.0f).asGroupedInts().toList()
                assertThat(l).size().isEqualTo(5)
                assertThat(l[0]).isEqualTo(listOf(0, 1, 2))
                assertThat(l[1]).isEqualTo(listOf(2, 3, 4))
                assertThat(l[2]).isEqualTo(listOf(4, 5, 6))
                assertThat(l[3]).isEqualTo(listOf(6, 7, 8))
                assertThat(l[4]).isEqualTo(listOf(8))
            }
        }

        describe("Partial windows.") {

            val windowedStream = (0..9).stream().window(3).sliding(2)

            it("should return values grouped by three, the rest as non-full list") {
                val l = windowedStream.asSequence(1.0f).asGroupedInts().toList()
                assertThat(l).size().isEqualTo(5)
                assertThat(l[0]).isEqualTo(listOf(0, 1, 2))
                assertThat(l[1]).isEqualTo(listOf(2, 3, 4))
                assertThat(l[2]).isEqualTo(listOf(4, 5, 6))
                assertThat(l[3]).isEqualTo(listOf(6, 7, 8))
                assertThat(l[4]).isEqualTo(listOf(8, 9))
            }
        }

        describe("No elements at all") {

            val windowedStream = IntStream(emptyList()).window(3).sliding(2)

            it("should return no values") {
                val l = windowedStream.asSequence(1.0f).asGroupedInts().toList()
                assertThat(l).size().isEqualTo(0)
            }
        }

        describe("Range projection. Sample rate 2Hz. Overall time 5 seconds") {

            /*
                 0 1 2 3 4 5 6 7 8 9
                 ^---^                 - window #0 with time 0-1499.99[9]ms
                     ^---^             - window #1 with time 1000-2499.99[9]ms
                         ^---^         - window #2 with time 2000-3499.99[9]ms
                             ^---^     - window #3 with time 3000-4499.99[9]ms
                                 ^-^   - window #4 with time 4000-4999.99[9]ms
            */
            val windowStream = (0..9).stream().window(3).sliding(2)
            val sampleRate = 2.0f

            describe("Coinciding with Window.") {
                val range = windowStream.rangeProjection(0, 1499)
                it("should return 1 window which it has intersection with") {
                    val l = range.asSequence(sampleRate).asGroupedInts().toList()
                    assertThat(l).size().isEqualTo(2)
                    assertThat(l).at(0).isEqualTo(listOf(0, 1, 2))
                    assertThat(l).at(1).isEqualTo(listOf(2, 3, 4))
                }
            }

            describe("Only one winow intersection.") {
                val range = windowStream.rangeProjection(0, 1)
                it("should return 1 window which it has intersection with") {
                    val l = range.asSequence(sampleRate).asGroupedInts().toList()
                    assertThat(l).size().isEqualTo(1)
                    assertThat(l).at(0).isEqualTo(listOf(0, 1, 2))
                }
            }

            describe("Not-coinciding with Window.") {
                val range = windowStream.rangeProjection(500, 1621)
                it("should return 2 windows") {
                    val l = range.asSequence(sampleRate).asGroupedInts().toList()
                    assertThat(l).size().isEqualTo(2)
                    assertThat(l).at(0).isEqualTo(listOf(0, 1, 2))
                    assertThat(l).at(1).isEqualTo(listOf(2, 3, 4))
                }
            }

            describe("Out-of-range. Before") {
                val range = windowStream.rangeProjection(-42, 500)
                it("should return 1 window") {
                    val l = range.asSequence(sampleRate).asGroupedInts().toList()
                    assertThat(l).size().isEqualTo(1)
                    assertThat(l).at(0).isEqualTo(listOf(0, 1, 2))
                }
            }

            describe("Out-of-range. After") {
                val range = windowStream.rangeProjection(4900, 500100)
                it("should return 1 window") {
                    val l = range.asSequence(sampleRate).asGroupedInts().toList()
                    assertThat(l).size().isEqualTo(1)
                    assertThat(l).at(0).isEqualTo(listOf(8, 9))
                }
            }
        }
    }
})

fun <T> Assert<List<T>>.at(idx: Int): Assert<T> = this.prop("[$idx]") { it[idx] }