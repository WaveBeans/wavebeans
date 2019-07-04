package mux.lib.math

import assertk.assertThat
import assertk.assertions.isCloseTo
import assertk.assertions.isEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object ComplexNumberSpec : Spek({
    describe("Given complex number `3 + 4i`") {
        val c = complex(3.0, 4.0)

        it("should be `6 + 8i` if added to itself") { assertThat(c + c).isEqualTo(6.r + 8.i) }
        it("should be `0 + 0i` if subtracted from itself") { assertThat(c - c).isEqualTo(0.r + 0.i) }
        it("should be `-7 + 24i` if multiplied by itself") { assertThat(c * c).isEqualTo(-7.r + 24.i) }
        it("should be `5` as abs()") { assertThat(c.abs()).isEqualTo(5.0) }
        it("should be `0.9272952180016122` as phi()") { assertThat(c.phi()).isCloseTo(0.9272952180016122, 1e-16) }
        it("should be `6 + 8i` if multiplied by real number 2 (right)") { assertThat(c * 2).isEqualTo(6 + 8.i) }
        it("should be `6 + 8i` if multiplied by real number 2 (left)") { assertThat(2 * c).isEqualTo(6 + 8.i) }
        it("should be `1.5 + 4i` if divided by real number 2 (left)") { assertThat(c / 2).isEqualTo(1.5 + 2.i) }

        describe("And another complex number `4 + 3i`") {
            val c2 = complex(4.0, 3.0)
            it("the sum c1 + c2 should be `7 + 7i`") { assertThat(c + c2).isEqualTo(7.r + 7.i) }
            it("the sum c2 + c1 should be `7 + 7i`") { assertThat(c2 + c).isEqualTo(7.r + 7.i) }
            it("the subtraction c1 - c2 should be `-1 + 1i`") { assertThat(c - c2).isEqualTo(-1.r + 1.i) }
            it("the subtraction c2 - 1 should be `1 - 1i`") { assertThat(c2 - c).isEqualTo(1.r - 1.i) }
            it("should be `5` as abs()") { assertThat(c2.abs()).isEqualTo(5.0) }
            it("should be `0.9272952180016122` as phi()") { assertThat(c2.phi()).isCloseTo(0.6435011087932844, 1e-16) }
            it("the product c1 * c2 should be `25i`") { assertThat(c * c2).isEqualTo(25.i) }
            it("the product c2 * c1 should be `25i`") { assertThat(c2 * c).isEqualTo(25.i) }
        }
    }

    describe("different ways to define complex number") {
        it("Define via `1   + 1.i`") { assertThat(1 + 1.i).isEqualTo(complex(1.0, 1.0)) }
        it("Define via `1.r + 1.i`") { assertThat(1.r + 1.i).isEqualTo(complex(1.0, 1.0)) }
        it("Define via `1   - 1.i`") { assertThat(1 - 1.i).isEqualTo(complex(1.0, -1.0)) }
        it("Define via `1.r - 1.i`") { assertThat(1.r - 1.i).isEqualTo(complex(1.0, -1.0)) }
        it("Define via `1.i + 1  `") { assertThat(1.i + 1).isEqualTo(complex(1.0, 1.0)) }
        it("Define via `1.i - 1  `") { assertThat(1.i - 1).isEqualTo(complex(-1.0, 1.0)) }
        it("Define via `1.i + 1.r`") { assertThat(1.i + 1.r).isEqualTo(complex(1.0, 1.0)) }
        it("Define via `1.i - 1.r`") { assertThat(1.i - 1.r).isEqualTo(complex(-1.0, 1.0)) }
    }
})