package mux.lib

import assertk.Assert
import assertk.all
import assertk.assertions.each
import assertk.assertions.isCloseTo
import assertk.assertions.isEqualTo
import assertk.assertions.support.expected
import assertk.assertions.support.show
import mux.lib.io.ByteArrayLittleEndianInput
import mux.lib.io.ByteArrayLittleEndianInputParams
import mux.lib.io.FiniteInput
import mux.lib.math.ComplexNumber
import mux.lib.math.minus
import mux.lib.math.plus
import mux.lib.stream.FiniteInputSampleStream
import mux.lib.stream.SampleStream
import mux.lib.stream.ZeroFilling
import mux.lib.stream.sampleStream
import org.spekframework.spek2.dsl.Skip
import org.spekframework.spek2.dsl.TestBody
import org.spekframework.spek2.style.specification.Suite

fun BeanStream<SampleArray, *>.listOfBytesAsInts(sampleRate: Float, samplesToRead: Int = Int.MAX_VALUE): List<Int> =
        this.asSequence(sampleRate)
                .flatMap { it.asSequence() }
                .take(samplesToRead)
                .map { it.asByte().toInt() and 0xFF }
                .toList()

fun BeanStream<SampleArray, *>.listOfShortsAsInts(sampleRate: Float, samplesToRead: Int = Int.MAX_VALUE): List<Int> =
        this.asSequence(sampleRate)
                .flatMap { it.asSequence() }
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
    val actualss = actual.asSequence(sampleRate).flatMap { it.asSequence() }.take(samplesToCompare).toList()
    val vss = v.asSequence(sampleRate).flatMap { it.asSequence() }.take(samplesToCompare).toList()
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
