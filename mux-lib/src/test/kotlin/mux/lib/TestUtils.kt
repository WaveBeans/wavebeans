package mux.lib

import assertk.Assert
import assertk.all
import assertk.assertions.isEqualTo
import assertk.assertions.support.expected
import assertk.assertions.support.show
import mux.lib.io.AudioInput
import mux.lib.math.ComplexNumber
import mux.lib.math.minus
import mux.lib.math.plus
import mux.lib.stream.SampleStream
import mux.lib.stream.asByte
import mux.lib.stream.asShort
import org.spekframework.spek2.dsl.Skip
import org.spekframework.spek2.dsl.TestBody
import org.spekframework.spek2.style.specification.Suite

fun SampleStream.listOfBytesAsInts(sampleRate: Float): List<Int> =
        this.asSequence(sampleRate)
                .map { it.asByte().toInt() and 0xFF }
                .toList()

fun AudioInput.listOfBytesAsInts(sampleRate: Float): List<Int> =
        this.asSequence(sampleRate)
                .map { it.asByte().toInt() and 0xFF }
                .toList()

fun AudioInput.listOfShortsAsInts(sampleRate: Float): List<Int> =
        this.asSequence(sampleRate)
                .map { it.asShort().toInt() and 0xFFFF }
                .toList()


fun <T> Assert<Iterable<T>>.eachIndexed(expectedSize: Int? = null, f: (Assert<T>, Int) -> Unit) = given { actual ->
    all {
        expectedSize?.let { assertThat(actual.count(), "elements count").isEqualTo(it) }
        actual.forEachIndexed { index, item ->
            f(assertThat(item, name = "${name ?: ""}${show(index, "[]")}"), index)
        }
    }
}

/**
 * Asserts the value if it is close to the expected value with given delta.
 */
fun Assert<ComplexNumber>.isCloseTo(value: ComplexNumber, delta: ComplexNumber) = given { actual ->
    if (actual >= value.minus(delta) && actual <= value.plus(delta)) return
    expected("${show(actual)} to be close to ${show(value)} with delta of ${show(delta)}, but was not")
}

fun Suite.itShouldHave(what: String, body: TestBody.() -> Unit) = this.it("should have $what", skip = Skip.No, body = body)
