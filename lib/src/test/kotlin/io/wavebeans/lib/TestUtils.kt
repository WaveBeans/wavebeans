package io.wavebeans.lib

import assertk.Assert
import assertk.all
import assertk.assertions.each
import assertk.assertions.isCloseTo
import assertk.assertions.isEqualTo
import assertk.assertions.prop
import assertk.assertions.support.expected
import assertk.assertions.support.fail
import assertk.assertions.support.show
import io.wavebeans.lib.*
import io.wavebeans.lib.io.ByteArrayLittleEndianInput
import io.wavebeans.lib.io.ByteArrayLittleEndianInputParams
import io.wavebeans.lib.io.FiniteInput
import io.wavebeans.lib.math.ComplexNumber
import io.wavebeans.lib.math.minus
import io.wavebeans.lib.math.plus
import io.wavebeans.lib.stream.FiniteInputSampleStream
import io.wavebeans.lib.stream.SampleStream
import io.wavebeans.lib.stream.ZeroFilling
import io.wavebeans.lib.stream.sampleStream
import io.wavebeans.lib.stream.window.Window
import org.spekframework.spek2.dsl.Skip
import org.spekframework.spek2.dsl.TestBody
import org.spekframework.spek2.style.specification.Suite
import java.util.concurrent.TimeUnit

fun BeanStream<Sample, *>.listOfBytesAsInts(sampleRate: Float, samplesToRead: Int = Int.MAX_VALUE): List<Int> =
        this.asSequence(sampleRate)
                .take(samplesToRead)
                .map { it.asByte().toInt() and 0xFF }
                .toList()

fun BeanStream<Sample, *>.listOfShortsAsInts(sampleRate: Float, samplesToRead: Int = Int.MAX_VALUE): List<Int> =
        this.asSequence(sampleRate)
                .take(samplesToRead)
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

fun Assert<SampleStream>.isCloseTo(v: SampleStream, sampleRate: Float, samplesToCompare: Int, delta: Double) = given { actual ->
    val actualss = actual.asSequence(sampleRate).take(samplesToCompare).toList()
    val vss = v.asSequence(sampleRate).take(samplesToCompare).toList()
    if (actualss.size - vss.size != 0) expected("The size should be the same", vss.size, actualss.size)
    val diff = actualss
            .zip(vss)
            .map { it.first - it.second }
    assertThat(diff).each { it.isCloseTo(0.0, delta) }
}

/**
 * Asserts the value if it is close to the expected value with given delta.
 */
fun Assert<ComplexNumber>.isCloseTo(value: ComplexNumber, delta: ComplexNumber) = given { actual ->
    if (actual >= value.minus(delta) && actual <= value.plus(delta)) return
    expected("${show(actual)} to be close to ${show(value)} with delta of ${show(delta)}, but was not")
}

fun Suite.itShouldHave(what: String, body: TestBody.() -> Unit) = this.it("should have $what", skip = Skip.No, body = body)

fun Iterable<Int>.stream(sampleRate: Float, bitDepth: BitDepth = BitDepth.BIT_8): SampleStream {
    return FiniteInputSampleStream(
            this.map {
                when (bitDepth) {
                    BitDepth.BIT_8 -> listOf(it.toByte())
                    BitDepth.BIT_16 -> listOf((it shr 8) and 0xFF, it and 0xFF).map { i -> i.toByte() }.reversed()
                    BitDepth.BIT_24 -> TODO()
                    BitDepth.BIT_32 -> listOf((it shr 24) and 0xFF, (it shr 16) and 0xFF, (it shr 8) and 0xFF, it and 0xFF).map { i -> i.toByte() }.reversed()
                    BitDepth.BIT_64 -> TODO()
                }
            }.flatten().toList().toByteArray().asInput(sampleRate, bitDepth),
            NoParams()
    ).sampleStream(ZeroFilling())
}

fun ByteArray.asInput(sampleRate: Float, bitDepth: BitDepth = BitDepth.BIT_8): FiniteInput =
        ByteArrayLittleEndianInput(ByteArrayLittleEndianInputParams(sampleRate, bitDepth, this))

fun <T> Int.repeat(f: (Int) -> T): List<T> = (0 until this).map { f(it) }

fun <T> Assert<List<T>>.isListOf(vararg expected: Any?) = given { actual ->
    if (actual == expected.toList()) return
    fail(expected, actual)
}

fun IntRange.stream() = IntStream(this.toList())

fun Sequence<Sample>.asInts() = this.map { it.asInt() }

fun Sequence<Window<Sample>>.asGroupedInts() = this.map { it.elements.map { it.asInt() } }

class IntStream(
        val seq: List<Int>,
        val start: Long = 0,
        val end: Long? = null,
        val timeUnit: TimeUnit = TimeUnit.MILLISECONDS
) : SampleStream {
    override fun asSequence(sampleRate: Float): Sequence<Sample> {
        val startIdx = timeToSampleIndexFloor(start, timeUnit, sampleRate).toInt()
        val endIdx = end?.let { timeToSampleIndexCeil(it, timeUnit, sampleRate).toInt() } ?: Int.MAX_VALUE
        return seq
                .drop(startIdx)
                .take(endIdx - startIdx)
                .asSequence().map { sampleOf(it) }
    }

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): IntStream = IntStream(seq, start, end, timeUnit)

    override fun inputs(): List<Bean<*, *>> = throw UnsupportedOperationException()

    override val parameters: BeanParams
        get() = throw UnsupportedOperationException()

}

fun <T> Assert<List<T>>.at(idx: Int): Assert<T> = this.prop("[$idx]") { it[idx] }
