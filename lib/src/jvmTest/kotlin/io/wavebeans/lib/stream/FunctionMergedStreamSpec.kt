package io.wavebeans.lib.stream

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.wavebeans.lib.stream
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import io.wavebeans.lib.*
import io.wavebeans.lib.io.input
import io.wavebeans.lib.io.inputWithSampleRate
import io.wavebeans.lib.stream.window.Window
import io.wavebeans.lib.stream.window.window

object FunctionMergedStreamSpec : Spek({
    describe("10 items int stream") {

        val source = (0..9).stream()

        describe("merged with the same size") {
            val merging = (10..19).stream()

            it("should return valid sum") {
                assertThat(source.merge(with = merging) { (x, y) -> x + y }.toListInt())
                        .isEqualTo((10..28 step 2).toList())
            }

            it("should return valid windows") {
                assertThat(source.merge(with = merging) { (x, y) -> windowOf(x, y) }.toListWindowInt())
                        .isListOf(
                                listOf(0, 10),
                                listOf(1, 11),
                                listOf(2, 12),
                                listOf(3, 13),
                                listOf(4, 14),
                                listOf(5, 15),
                                listOf(6, 16),
                                listOf(7, 17),
                                listOf(8, 18),
                                listOf(9, 19)
                        )
            }


            it("should return valid values after summing up of 3 streams consequently") {
                val anotherMerging = (20..29).stream()
                assertThat(source
                        .merge(with = merging) { (x, y) -> x + y }
                        .merge(with = anotherMerging) { (x, y) -> x + y }
                        .toListInt()
                ).isEqualTo((30..58 step 3).toList())
            }
        }

        describe("merged with the smaller size") {
            val merging = (10..15).stream()

            it("should return valid sum") {
                assertThat(source.merge(with = merging) { (x, y) -> x + y }.toListInt())
                        .isEqualTo((10..20 step 2).toList() + (6..9))

            }

            it("should return valid windows") {
                assertThat(source.merge(with = merging) { (x, y) -> windowOf(x, y) }.toListWindowInt())
                        .isListOf(
                                listOf(0, 10),
                                listOf(1, 11),
                                listOf(2, 12),
                                listOf(3, 13),
                                listOf(4, 14),
                                listOf(5, 15),
                                listOf(6, 0),
                                listOf(7, 0),
                                listOf(8, 0),
                                listOf(9, 0)
                        )
            }
        }

        describe("merged with the larger size") {
            val merging = (10..25).stream()

            it("should return valid sum") {
                assertThat(source.merge(with = merging) { (x, y) -> x + y }.toListInt())
                        .isEqualTo((10..28 step 2).toList() + (20..25))

            }

            it("should return valid windows") {
                assertThat(source.merge(with = merging) { (x, y) -> windowOf(x, y) }.toListWindowInt())
                        .isListOf(
                                listOf(0, 10),
                                listOf(1, 11),
                                listOf(2, 12),
                                listOf(3, 13),
                                listOf(4, 14),
                                listOf(5, 15),
                                listOf(6, 16),
                                listOf(7, 17),
                                listOf(8, 18),
                                listOf(9, 19),
                                listOf(0, 20),
                                listOf(0, 21),
                                listOf(0, 22),
                                listOf(0, 23),
                                listOf(0, 24),
                                listOf(0, 25)
                        )
            }
        }

        describe("merged with the infinite size stream") {
            val merging = input { (i, _) -> sampleOf((i + 10).toInt()) }

            it("should return valid sum") {
                assertThat(source.merge(with = merging) { (x, y) -> x + y }.toListInt(take = 20))
                        .isEqualTo((10..28 step 2).toList() + (20..29))

            }

            it("should return valid windows") {
                assertThat(source.merge(with = merging) { (x, y) -> windowOf(x, y) }.toListWindowInt(take = 20))
                        .isListOf(
                                listOf(0, 10),
                                listOf(1, 11),
                                listOf(2, 12),
                                listOf(3, 13),
                                listOf(4, 14),
                                listOf(5, 15),
                                listOf(6, 16),
                                listOf(7, 17),
                                listOf(8, 18),
                                listOf(9, 19),
                                listOf(0, 20),
                                listOf(0, 21),
                                listOf(0, 22),
                                listOf(0, 23),
                                listOf(0, 24),
                                listOf(0, 25),
                                listOf(0, 26),
                                listOf(0, 27),
                                listOf(0, 28),
                                listOf(0, 29),
                        )
            }
        }
    }

    describe("Int and float stream") {
        val stream = input { (idx, _) -> idx.toInt() }
                .merge(input { (idx, _) -> idx.toFloat() }) { (a, b) ->
                    requireNotNull(a)
                    requireNotNull(b)
                    a.toLong() + b.toLong()
                }

        it("should return list of sum of long values") {
            assertThat(stream.asSequence(10.0f).take(10).toList())
                    .isEqualTo((0 until 20 step 2).map { it.toLong() })
        }
    }

    describe("Int and Window<Int> stream") {
        val stream = input { (idx, _) -> idx.toInt() }
                .window(2) { 0 }
                .merge(input { (idx, _) -> idx.toInt() }) { (window, a) ->
                    requireNotNull(window)
                    requireNotNull(a)
                    window.elements.first().toLong() + a.toLong()
                }

        it("should return list of sum of long values") {
            assertThat(stream.asSequence(10.0f).take(10).toList())
                    .isEqualTo((0 until 30 step 3).map { it.toLong() })
        }
    }
})

private fun windowOf(x: Sample?, y: Sample?) =
        Window.ofSamples(2, 2, listOf(x ?: ZeroSample, y ?: ZeroSample))

private fun BeanStream<Sample>.toListInt(take: Int = Int.MAX_VALUE) =
        this.asSequence(1.0f).take(take).map { it.asInt() }.toList()

private fun BeanStream<Window<Sample>>.toListWindowInt(take: Int = Int.MAX_VALUE) =
        this.asSequence(1.0f).take(take).map { it.elements.map { it.asInt() } }.toList()