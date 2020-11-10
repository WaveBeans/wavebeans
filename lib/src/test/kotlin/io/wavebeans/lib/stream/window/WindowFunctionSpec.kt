package io.wavebeans.lib.stream.window

import assertk.assertThat
import assertk.assertions.each
import assertk.assertions.isEqualTo
import assertk.assertions.prop
import io.wavebeans.lib.io.input
import io.wavebeans.lib.isListOf
import io.wavebeans.lib.sampleOf
import io.wavebeans.lib.sampleVectorOf
import io.wavebeans.lib.seqStream
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos

object WindowFunctionSpec : Spek({
    describe("Rectangle window function") {

        it("should remain signal the same over the window with step = size") {
            val w = seqStream()
                    .window(10)
                    .rectangle()
                    .asSequence(1.0f)
                    .take(2)
                    .toList()

            assertThat(w).isEqualTo(
                    seqStream()
                            .window(10)
                            .asSequence(1.0f)
                            .take(2)
                            .toList()
            )
        }

        it("should remain signal the same over the window with step < size") {
            val w = seqStream()
                    .window(10, 5)
                    .rectangle()
                    .asSequence(1.0f)
                    .take(2)
                    .toList()

            assertThat(w).isEqualTo(
                    seqStream()
                            .window(10, 5)
                            .asSequence(1.0f)
                            .take(2)
                            .toList()
            )
        }

        it("should remain signal the same over the window with step > size") {
            val w = seqStream()
                    .window(10, 15)
                    .rectangle()
                    .asSequence(1.0f)
                    .take(2)
                    .toList()

            assertThat(w).isEqualTo(
                    seqStream()
                            .window(10, 15)
                            .asSequence(1.0f)
                            .take(2)
                            .toList()
            )
        }
    }

    describe("Triangular window function") {
        val n = 50
        val triangularValues = (0 until n).map { i ->
            val halfN = n / 2.0
            sampleOf(1.0 - abs((i - halfN) / halfN))
        }

        it("should return all blackman values inside windows") {
            val tries = 3
            val w = input { sampleOf(1.0) }
                    .window(n)
                    .triangular()
                    .asSequence(1.0f)
                    .take(tries)
                    .toList()

            assertThat(w).each { v ->
                v.prop("elements") { it.elements }.isEqualTo(triangularValues)
            }
        }
        it("should create a sample vector") {
            val o = sampleVectorOf(n, ::triangularFunc)
            assertThat(o).isEqualTo(sampleVectorOf(triangularValues))
        }
        it("should read as sequence") {
            val o = triangularFunc(n).toList()
            assertThat(o).isEqualTo(triangularValues)
        }
    }

    describe("Blackman window function") {
        val n = 50
        val blackmanValues = (0 until n).map { i ->
            val a0 = 0.42
            val a1 = 0.5
            val a2 = 0.08
            sampleOf(a0 - a1 * cos(2 * PI * i / n) + a2 * cos(4 * PI * i / n))
        }

        it("should return all blackman values inside windows") {
            val tries = 3
            val w = input { sampleOf(1.0) }
                    .window(n)
                    .blackman()
                    .asSequence(1.0f)
                    .take(tries)
                    .toList()

            assertThat(w).each { v ->
                v.prop("elements") { it.elements }.isEqualTo(blackmanValues)
            }
        }
        it("should create a sample vector") {
            val o = sampleVectorOf(n, ::blackmanFunc)
            assertThat(o).isEqualTo(sampleVectorOf(blackmanValues))
        }
        it("should read as sequence") {
            val o = blackmanFunc(n).toList()
            assertThat(o).isEqualTo(blackmanValues)
        }
    }

    describe("Hamming window function") {
        val n = 50

        val hammingValues = (0 until n).map { i ->
            val a0 = 25.0 / 46.0
            sampleOf(a0 - (1 - a0) * cos(2 * PI * i / n))
        }

        it("should return all blackman values inside windows") {
            val tries = 3
            val w = input { sampleOf(1.0) }
                    .window(n)
                    .hamming()
                    .asSequence(1.0f)
                    .take(tries)
                    .toList()

            assertThat(w).each { v ->
                v.prop("elements") { it.elements }.isEqualTo(hammingValues)
            }
        }
        it("should create a sample vector") {
            val o = sampleVectorOf(n, ::hammingFunc)
            assertThat(o).isEqualTo(sampleVectorOf(hammingValues))
        }
        it("should read as sequence") {
            val o = hammingFunc(n).toList()
            assertThat(o).isEqualTo(hammingValues)
        }
    }

    describe("Custom window function") {
        val w = seqStream()
                .window(10)
                .windowFunction { sampleOf(2.0) }
                .asSequence(1.0f)
                .take(2)
                .toList()

        it("should return all doubled values inside windows") {
            assertThat(w).isEqualTo(
                    seqStream()
                            .window(10)
                            .asSequence(1.0f)
                            .map { window -> window.copy(elements = window.elements.map { it * 2 }) }
                            .take(2)
                            .toList()
            )
        }
    }

    describe("Custom type window function") {
        val w = input { (i, _) -> i }
                .window(5) { 0 }
                .windowFunction(
                        func = { 2 },
                        multiplyFn = { (a, b) -> a * b }
                )
                .asSequence(1.0f)
                .take(2)
                .toList()

        it("should return all values inside windows") {
            assertThat(w).isListOf(
                    Window(5, 5, listOf(0L, 2L, 4L, 6L, 8L)) { 0L },
                    Window(5, 5, listOf(10L, 12L, 14L, 16L, 18L)) { 0L }
            )
        }
    }
})
