package io.wavebeans.lib.io

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.message
import assertk.catch
import io.wavebeans.lib.isListOf
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object ListAsInputSpec : Spek({
    describe("List of Ints") {
        val x = listOf(1, 2, 3, 4)

        it("should return values as stream") {
            assertThat(x.input().asSequence(1234.0f).toList()).isListOf(1, 2, 3, 4)
        }
        it("should not matter the sample rate") {
            assertThat(x.input().asSequence(1234.0f).toList())
                    .isEqualTo(x.input().asSequence(2345.0f).toList())
        }
        it("should now allow create input with empty list") {
            assertThat(catch { emptyList<Int>().input() })
                    .isNotNull()
                    .message().isEqualTo("Input list should not be empty")
        }
    }
    describe("List of objects") {
        data class A(val v: String, val f: Float)

        val x = listOf(1, 2, 3, 4).map { A(it.toString(), it.toFloat()) }

        it("should return values as stream") {
            assertThat(x.input().asSequence(1234.0f).toList()).isEqualTo(x)
        }
    }
})