package mux.lib.io

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.matchesPredicate
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.lang.Math.abs

object SineGeneratedInputSpec : Spek({
    describe("Sinusoid of A=1.0, f=10.0, phi=1.0, fs=50.0 and t=0.1") {
        val generator = SineGeneratedInput(
                50.0f,
                10.0,
                1.0,
                0.1,
                1.0
        )

        describe("generates sequence") {
            val seq = generator.asSequence(50.0f).toList()

            it("should be 5 samples array") {
                val expected = arrayOf(0.54030231, -0.63332387, -0.93171798, 0.05749049, 0.96724906)
                assertThat(expected.size, "size of arrays").isEqualTo(seq.size)
                (0 until expected.size).forEach { i ->
                    assertThat(expected[i]).matchesPredicate {
                        abs(it - seq[i]) < 0.00001
                    }
                }
            }
        }
    }
})