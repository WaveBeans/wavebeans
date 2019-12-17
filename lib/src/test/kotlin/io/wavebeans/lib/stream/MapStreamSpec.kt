package io.wavebeans.lib.stream

import assertk.assertThat
import io.wavebeans.lib.*
import io.wavebeans.lib.stream.window.window
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object MapStreamSpec : Spek({
    describe("Sample to Sample mapping") {
        val stream = (0..5).stream()

        it("should have doubled value if multiply every sample by 2") {
            assertThat(stream.map { it * 2 }.toListInt()).isListOf(0, 2, 4, 6, 8, 10)
        }

        it("should have values no bigger than 3") {
            assertThat(stream.map { if (it >= sampleOf(3)) sampleOf(3) else it }.toListInt()).isListOf(0, 1, 2, 3, 3, 3)
        }
    }

    describe("Sample to another type mapping") {
        val stream = (0..5).stream()

        it("should map to string values of int samples") {
            assertThat(stream.map { it.asInt().toString() }.asSequence(1.0f).toList()).isListOf("0", "1", "2", "3", "4", "5")
        }
    }

    describe("Window<Sample> to Sample mapping") {
        val stream = (0..5).stream().window(2)

        it("should have only first elements of window") {
            assertThat(stream.map { it.elements.first() }.toListInt()).isListOf(0, 2, 4)
        }

        it("should have only last elements of window") {
            assertThat(stream.map { it.elements.last() }.toListInt()).isListOf(1, 3, 5)
        }

    }
})

private fun BeanStream<Sample>.toListInt() = this.asSequence(1.0f).map { it.asInt() }.toList()