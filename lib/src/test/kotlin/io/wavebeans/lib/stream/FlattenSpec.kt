package io.wavebeans.lib.stream

import assertk.all
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.prop
import assertk.assertions.size
import io.wavebeans.lib.*
import io.wavebeans.lib.io.input
import io.wavebeans.lib.stream.window.Window
import io.wavebeans.lib.stream.window.window
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object FlattenSpec : Spek({
    describe("Flatten list of integers") {
        it("should flatten the stream of lists") {
            val l = input { (i, _) ->
                when (i) {
                    0L -> listOf(1, 2, 3)
                    1L -> listOf(4)
                    2L -> listOf()
                    3L -> listOf()
                    4L -> listOf()
                    5L -> listOf()
                    6L -> listOf(5, 6)
                    7L -> listOf(7)
                    8L -> listOf()
                    else -> null
                }
            }
                    .flatten()
                    .asSequence(1.0f)
                    .toList()
            assertThat(l).isListOf(1, 2, 3, 4, 5, 6, 7)
        }

        it("should flatten the stream of lists with duplicates") {
            val l = input { (i, _) ->
                when (i) {
                    0L -> listOf(1, 2, 3, 3, 4)
                    1L -> listOf(4)
                    2L -> listOf()
                    3L -> listOf(5)
                    4L -> listOf()
                    5L -> listOf()
                    6L -> listOf(5, 6, 7, 7)
                    7L -> listOf(7)
                    8L -> listOf()
                    else -> null
                }
            }
                    .flatten()
                    .asSequence(1.0f)
                    .toList()
            assertThat(l).isListOf(1, 2, 3, 3, 4, 4, 5, 5, 6, 7, 7, 7)
        }

        it("should flatten the empty stream of lists") {
            val l = input<List<Int>> { null }
                    .flatten()
                    .asSequence(1.0f)
                    .toList()
            assertThat(l).isEmpty()
        }

        it("should flatten the stream of empty lists") {
            val l = input<List<Int>> { (i, _) ->
                when (i) {
                    0L -> listOf()
                    1L -> listOf()
                    2L -> listOf()
                    3L -> listOf()
                    4L -> listOf()
                    5L -> listOf()
                    6L -> listOf()
                    7L -> listOf()
                    8L -> listOf()
                    else -> null
                }
            }
                    .flatten()
                    .asSequence(1.0f)
                    .toList()
            assertThat(l).isEmpty()
        }

        it("should flatten the stream of lists but containing only even values") {
            val l = input { (i, _) ->
                when (i) {
                    0L -> listOf(1, 2, 3)
                    1L -> listOf(4)
                    2L -> listOf()
                    3L -> listOf()
                    4L -> listOf()
                    5L -> listOf()
                    6L -> listOf(5, 6)
                    7L -> listOf(7)
                    8L -> listOf()
                    else -> null
                }
            }
                    .flatMap { it.filter { it % 2 == 0 } }
                    .asSequence(1.0f)
                    .toList()
            assertThat(l).isListOf(2, 4, 6)
        }
    }

    describe("Flatten stream of sample vectors") {
        it("should flatten the stream of lists") {
            val l = input { (i, _) ->
                when (i) {
                    0L -> listOf(1, 2, 3)
                    1L -> listOf(4)
                    2L -> listOf()
                    3L -> listOf()
                    4L -> listOf()
                    5L -> listOf()
                    6L -> listOf(5, 6)
                    7L -> listOf(7)
                    8L -> listOf()
                    else -> null
                }
                        ?.map { sampleOf(it) }
                        ?.let { sampleVectorOf(it) }
            }
                    .flatten()
                    .asSequence(1.0f)
                    .toList()

            assertThat(l).isListOf(
                    sampleOf(1),
                    sampleOf(2),
                    sampleOf(3),
                    sampleOf(4),
                    sampleOf(5),
                    sampleOf(6),
                    sampleOf(7)
            )
        }
    }

    describe("Flatten windowed stream") {
        describe("Stream of ints") {
            it("should flatten windows if step == size") {
                val l = input { (i, _) -> if (i < 10) i.toInt() else null }
                        .window(2) { 0 }
                        .flatten()
                        .asSequence(1.0f)
                        .toList()

                assertThat(l).isListOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
            }
            it("should flatten windows with various sizes if step == size") {
                val l = input { (i, _) ->
                    when (i) {
                        0L -> Window(3, 3, listOf(0, 1, 2)) { 0 }
                        1L -> Window(2, 2, listOf(3, 4)) { 0 }
                        2L -> Window(4, 4, listOf(5, 6, 7, 8)) { 0 }
                        3L -> Window(1, 1, listOf(9)) { 0 }
                        else -> null
                    }
                }
                        .flatten()
                        .asSequence(1.0f)
                        .toList()

                assertThat(l).isListOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
            }
            it("should flatten windows if step < size") {
                val l = input { (i, _) -> if (i < 10) i.toInt() else null }
                        .window(3, 2) { -1 }
                        .flatten { (a, b) -> a + b }
                        .asSequence(1.0f)
                        .toList()

                assertThat(l).isListOf(0, 1, 4, 3, 8, 5, 12, 7, 16, 9, -1)
            }
            it("should flatten windows with various sizes if step < size") {
                /**
                 * 0 1 2
                 *     3 4
                 *       5 6  7  8
                 *            9 10
                 * --------------
                 * 0 1 5 9 6 16 18
                 */
                val l = input { (i, _) ->
                    when (i) {
                        0L -> Window(3, 2, listOf(0, 1, 2)) { 0 }
                        1L -> Window(2, 1, listOf(3, 4)) { 0 }
                        2L -> Window(4, 2, listOf(5, 6, 7, 8)) { 0 }
                        3L -> Window(2, 1, listOf(9, 10)) { 0 }
                        else -> null
                    }
                }
                        .flatten { (a, b) -> a + b }
                        .asSequence(1.0f)
                        .toList()

                assertThat(l).isListOf(0, 1, 5, 9, 6, 16, 18)
            }
            it("should flatten windows if step > size") {
                val l = input { (i, _) -> if (i < 10) i.toInt() else null }
                        .window(3, 4) { -1 }
                        .flatten()
                        .asSequence(1.0f)
                        .toList()

                assertThat(l).isListOf(0, 1, 2, -1, 4, 5, 6, -1, 8, 9, -1, -1)
            }
            it("should flatten windows with various sizes if step > size") {
                val l = input { (i, _) ->
                    when (i) {
                        0L -> Window(3, 4, listOf(0, 1, 2)) { -1 }
                        1L -> Window(2, 3, listOf(3, 4)) { -1 }
                        2L -> Window(4, 6, listOf(5, 6, 7, 8)) { -1 }
                        3L -> Window(2, 3, listOf(9, 10)) { -1 }
                        else -> null
                    }
                }
                        .flatten { (a, b) -> a + b }
                        .asSequence(1.0f)
                        .toList()

                assertThat(l).isListOf(0, 1, 2, -1, 3, 4, -1, 5, 6, 7, 8, -1, -1, 9, 10, -1)
            }
        }

        describe("stream of samples") {
            it("should flatten windows with step == size") {
                val l = seqStream()
                        .window(64)
                        .flatten()
                        .asSequence(1.0f)
                        .take(1024)
                        .toList()

                assertThat(l).isEqualTo(seqStream().asSequence(1.0f).take(1024).toList())
            }

            it("should flatten windows with step < size") {
                val l = input { (i, _) -> if (i < 10) sampleOf(1e-9 * (i + 1)) else null }
                        .window(3, 2)
                        .flatten()
                        .asSequence(1.0f)
                        .toList()

                assertThat(l).isListOf(
                        sampleOf(1 * 1e-9),
                        sampleOf(2 * 1e-9),
                        sampleOf(3 * 1e-9) + sampleOf(3 * 1e-9),
                        sampleOf(4 * 1e-9),
                        sampleOf(5 * 1e-9) + sampleOf(5 * 1e-9),
                        sampleOf(6 * 1e-9),
                        sampleOf(7 * 1e-9) + sampleOf(7 * 1e-9),
                        sampleOf(8 * 1e-9),
                        sampleOf(9 * 1e-9) + sampleOf(9 * 1e-9),
                        sampleOf(10 * 1e-9),
                        ZeroSample
                )
            }

            it("should flatten windows with step > size") {
                val l = seqStream()
                        .window(4, 6)
                        .flatten()
                        .asSequence(1.0f)
                        .take(40)
                        .toList()

                assertThat(l).isEqualTo(
                        seqStream().asSequence(1.0f)
                                .mapIndexed { index, sample -> if (index % 6 < 4) sample else ZeroSample }
                                .take(40)
                                .toList()
                )
            }
        }

        describe("stream of sample vectors") {
            it("should flatten window with step == size") {
                val l = seqStream()
                        .window(4).map { sampleVectorOf(it) }
                        .window(4) { EmptySampleVector }
                        .flatten()
                        .flatten()
                        .asSequence(1.0f)
                        .take(40)
                        .toList()

                assertThat(l).isEqualTo(seqStream().asSequence(1.0f).take(40).toList())
            }
            it("should flatten window with step < size") {
                val l = input { (i, _) -> if (i < 12) sampleOf(1e-9 * (i + 1)) else null }
                        .window(2).map { sampleVectorOf(it).also { println(it.contentToString()) } }
                        .window(3, 2) { EmptySampleVector }
                        .flatten()
                        .asSequence(1.0f)
                        .toList()

                assertThat(l).all {
                    size().isEqualTo(7)
                    prop("0") { it[0] }.isEqualTo(sampleVectorOf(sampleOf(1 * 1e-9), sampleOf(2 * 1e-9)))
                    prop("1") { it[1] }.isEqualTo(sampleVectorOf(sampleOf(3 * 1e-9), sampleOf(4 * 1e-9)))
                    prop("2") { it[2] }.isEqualTo(
                            sampleVectorOf(sampleOf(5 * 1e-9), sampleOf(6 * 1e-9)) +
                                    sampleVectorOf(sampleOf(5 * 1e-9), sampleOf(6 * 1e-9))
                    )
                    prop("3") { it[3] }.isEqualTo(sampleVectorOf(sampleOf(7 * 1e-9), sampleOf(8 * 1e-9)))
                    prop("4") { it[4] }.isEqualTo(
                            sampleVectorOf(sampleOf(9 * 1e-9), sampleOf(10 * 1e-9)) +
                                    sampleVectorOf(sampleOf(9 * 1e-9), sampleOf(10 * 1e-9))
                    )
                    prop("5") { it[5] }.isEqualTo(sampleVectorOf(sampleOf(11 * 1e-9), sampleOf(12 * 1e-9)))
                    prop("6") { it[6] }.isEqualTo(sampleVectorOf())
                }
            }
            it("should flatten window with step > size") {
                val l = input { (i, _) -> if (i < 12) sampleOf(1e-9 * (i + 1)) else null }
                        .window(2).map { sampleVectorOf(it).also { println(it.contentToString()) } }
                        .window(3, 4) { EmptySampleVector }
                        .flatten()
                        .asSequence(1.0f)
                        .toList()

                assertThat(l).all {
                    size().isEqualTo(8)
                    prop("0") { it[0] }.isEqualTo(sampleVectorOf(sampleOf(1 * 1e-9), sampleOf(2 * 1e-9)))
                    prop("1") { it[1] }.isEqualTo(sampleVectorOf(sampleOf(3 * 1e-9), sampleOf(4 * 1e-9)))
                    prop("2") { it[2] }.isEqualTo(sampleVectorOf(sampleOf(5 * 1e-9), sampleOf(6 * 1e-9)))
                    prop("3") { it[3] }.isEqualTo(sampleVectorOf())
                    prop("4") { it[4] }.isEqualTo(sampleVectorOf(sampleOf(9 * 1e-9), sampleOf(10 * 1e-9)))
                    prop("5") { it[5] }.isEqualTo(sampleVectorOf(sampleOf(11 * 1e-9), sampleOf(12 * 1e-9)))
                    prop("6") { it[6] }.isEqualTo(sampleVectorOf())
                    prop("7") { it[7] }.isEqualTo(sampleVectorOf())
                }
            }
        }
    }
})