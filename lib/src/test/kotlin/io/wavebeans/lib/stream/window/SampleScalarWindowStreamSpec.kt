package io.wavebeans.lib.stream.window

import assertk.Assert
import assertk.all
import assertk.assertThat
import assertk.assertions.support.fail
import io.wavebeans.lib.*
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object SampleScalarWindowStreamSpec : Spek({
    describe("Fixed window size=2") {
        val stream = DoubleStream(listOf(0.1, 0.2, 0.3, 0.4)).window(2)

        it("should be calculated as sum with scalar") {
            val l = (stream + 1.0).asSequence(2.0f).asGroupedDoubles().toList()
            assertThat(l).all {
                at(0).isListOf(1.1, 1.2)
                at(1).isListOf(1.3, 1.4)
            }
        }

        it("should be calculated as subtraction of scalar") {
            val l = (stream - 0.1).asSequence(2.0f).asGroupedDoubles().toList()
            assertThat(l).all {
                at(0).isListOf(0.0, 0.1)
                at(1).isListOf(0.2, 0.3)
            }
        }

        it("should be calculated as multiplication on scalar") {
            val l = (stream * 2).asSequence(2.0f).asGroupedDoubles().toList()
            assertThat(l).all {
                at(0).isListOf(0.2, 0.4)
                at(1).isListOf(0.6, 0.8)
            }
        }

        it("should be calculated as division on scalar") {
            val l = (stream / 2).asSequence(2.0f).asGroupedDoubles().toList()
            assertThat(l).all {
                at(0).isListOf(0.05, 0.1)
                at(1).isListOf(0.15, 0.2)
            }
        }
    }

    describe("Sliding window size=3 step=2") {
        val stream = DoubleStream(listOf(0.1, 0.2, 0.3, 0.4)).window(3).sliding(2)

        it("should be calculated as sum with scalar") {
            val l = (stream + 1.0).asSequence(2.0f).asGroupedDoubles().toList()
            assertThat(l).all {
                at(0).isListOf(1.1, 1.2, 1.3)
                at(1).isListOf(1.3, 1.4)
            }
        }

        it("should be calculated as subtraction of scalar") {
            val l = (stream - 0.1).asSequence(2.0f).asGroupedDoubles().toList()
            assertThat(l).all {
                at(0).isListOf(0.0, 0.1, 0.2)
                at(1).isListOf(0.2, 0.3)
            }
        }

        it("should be calculated as multiplication on scalar") {
            val l = (stream * 2).asSequence(2.0f).asGroupedDoubles().toList()
            assertThat(l).all {
                at(0).isListOf(0.2, 0.4, 0.6)
                at(1).isListOf(0.6, 0.8)
            }
        }

        it("should be calculated as division on scalar") {
            val l = (stream / 2).asSequence(2.0f).asGroupedDoubles().toList()
            assertThat(l).all {
                at(0).isListOf(0.05, 0.1, 0.15)
                at(1).isListOf(0.15, 0.2)
            }
        }
    }
})

private fun Assert<List<Sample>>.isListOf(vararg expected: Double) = given { actual ->
    if (actual.zip(expected.toList()).map { (a, b) -> kotlin.math.abs(a - b) }.all { it < 1e-10 }) return
    fail(expected, actual)
}

