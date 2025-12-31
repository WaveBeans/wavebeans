package io.wavebeans.lib.io

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.wavebeans.lib.asInt
import io.wavebeans.lib.sampleOf
import io.kotest.core.spec.style.DescribeSpec

class FunctionInputSpec : DescribeSpec({
    describe("Sequence of integers") {

        it("should generate 10 integers if it returns only that") {
            val seq = input { x, _ -> if (x < 10) sampleOf(x.toInt()) else null }
                .asSequence(44100.0f)
                .map { it.asInt() }
                .take(100)
                .toList()
            assertThat(seq).isEqualTo((0..9).toList())
        }

        it("should generate 100 integers as input doesn't limit it") {
            val seq = input { x, _ -> sampleOf(x.toInt()) }
                .asSequence(44100.0f)
                .map { it.asInt() }
                .take(100)
                .toList()
            assertThat(seq).isEqualTo((0..99).toList())
        }
    }
})
