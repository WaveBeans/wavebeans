package mux.lib

import assertk.assertThat
import assertk.assertions.isLessThan
import mux.lib.math.*
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object DftSpec : Spek({
    describe("Given signal as array of doubles [1,4]") {
        val x = (1..4).map { it.toDouble() }

        describe("Calculating DFT") {
            val dft = dft(x)

            it("should be as specific array") {
                val expected = arrayOf(
                        10 + 0.i,
                        -2 + 2.i,
                        -2 + 0.i,
                        -2 - 2.i
                )
                assertThat(dft.toList()).eachIndexed(4) { v, i ->
                    v.transform { (it - expected[i]).abs() }.isLessThan(1e-14)
                }
            }
        }

        describe("fft") {
            print(fft(x).map { it.string() })
        }
    }

    describe("Given DFT=[1,1,1,1]") {
        val x = listOf(1.0, 1.0, 1.0, 1.0)

        describe("Calculating IDFT") {
            val idft = idft(x)
            it("should be as specific array") {
                val expected = arrayOf(
                        1 + 0.i,
                        0 + 0.i,
                        0 + 0.i,
                        0 - 0.i
                )
                assertThat(idft.toList()).eachIndexed(4) { v, i ->
                    v.transform { (it - expected[i]).abs() }.isLessThan(1e-14)
                }
            }
        }
    }
})