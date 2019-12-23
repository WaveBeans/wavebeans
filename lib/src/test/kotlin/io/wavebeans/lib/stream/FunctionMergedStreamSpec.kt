package io.wavebeans.lib.stream

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.wavebeans.lib.stream
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import io.wavebeans.lib.*
import io.wavebeans.lib.stream.window.Window

object FunctionMergedStreamSpec : Spek({
    describe("10 items int stream") {

        val source = (0..9).stream()

        describe("merged with the same size") {
            val merging = (10..19).stream()

            it("should return valid sum") {
                assertThat(source.merge(with = merging) { x, y -> x + y }.toListInt())
                        .isEqualTo((10..28 step 2).toList())
            }

            it("should return valid windows") {
                assertThat(source.merge(with = merging) { x, y -> windowOf(x, y) }.toListWindowInt())
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
        }

        describe("merged with the smaller size") {
            val merging = (10..15).stream()

            it("should return valid sum") {
                assertThat(source.merge(with = merging) { x, y -> x + y }.toListInt())
                        .isEqualTo((10..20 step 2).toList() + (6..9))

            }

            it("should return valid windows") {
                assertThat(source.merge(with = merging) { x, y -> windowOf(x, y) }.toListWindowInt())
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
                assertThat(source.merge(with = merging) { x, y -> x + y }.toListInt())
                        .isEqualTo((10..28 step 2).toList() + (20..25))

            }

            it("should return valid windows") {
                assertThat(source.merge(with = merging) { x, y -> windowOf(x, y) }.toListWindowInt())
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
    }
})

private fun windowOf(x: Sample?, y: Sample?) =
        Window.ofSamples(2, 2, listOf(x ?: ZeroSample, y ?: ZeroSample))

private fun BeanStream<Sample>.toListInt() = this.asSequence(1.0f).map { it.asInt() }.toList()
private fun BeanStream<Window<Sample>>.toListWindowInt() = this.asSequence(1.0f).map { it.elements.map { it.asInt() } }.toList()