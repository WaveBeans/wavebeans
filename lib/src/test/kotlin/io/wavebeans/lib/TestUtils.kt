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
import io.wavebeans.lib.io.ByteArrayLittleEndianInput
import io.wavebeans.lib.io.ByteArrayLittleEndianInputParams
import io.wavebeans.lib.io.FiniteInput
import io.wavebeans.lib.io.StreamInput
import io.wavebeans.lib.math.ComplexNumber
import io.wavebeans.lib.math.minus
import io.wavebeans.lib.math.plus
import io.wavebeans.lib.stream.FiniteInputStream
import io.wavebeans.lib.stream.AfterFilling
import io.wavebeans.lib.stream.stream
import io.wavebeans.lib.stream.window.Window
import org.spekframework.spek2.dsl.Skip
import org.spekframework.spek2.dsl.TestBody
import org.spekframework.spek2.style.specification.Suite
import java.util.concurrent.TimeUnit

fun BeanStream<Sample>.listOfBytesAsInts(sampleRate: Float, samplesToRead: Int = Int.MAX_VALUE): List<Int> =
        this.asSequence(sampleRate)
                .take(samplesToRead)
                .map { it.asByte().asUnsignedByte() }
                .toList()

fun BeanStream<Sample>.listOfShortsAsInts(sampleRate: Float, samplesToRead: Int = Int.MAX_VALUE): List<Int> =
        this.asSequence(sampleRate)
                .take(samplesToRead)
                .map { it.asShort().toInt() }
                .toList()


fun Assert<BeanStream<Sample>>.isCloseTo(v: BeanStream<Sample>, sampleRate: Float, samplesToCompare: Int, delta: Double) = given { actual ->
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

fun Iterable<Int>.stream(sampleRate: Float, bitDepth: BitDepth = BitDepth.BIT_8): BeanStream<Sample> {
    return FiniteInputStream(
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
    ).stream(AfterFilling(ZeroSample))
}

fun ByteArray.asInput(sampleRate: Float, bitDepth: BitDepth = BitDepth.BIT_8): FiniteInput<Sample> =
        ByteArrayLittleEndianInput(ByteArrayLittleEndianInputParams(sampleRate, bitDepth, this))

fun <T> Int.repeat(f: (Int) -> T): List<T> = (0 until this).map { f(it) }

fun <T> Assert<List<T>>.isListOf(vararg expected: Any?) = given { actual ->
    val expectedAsList = expected.toList()
    if (actual == expectedAsList) return
    fail(expectedAsList, actual)
}

fun IntRange.stream() = IntStream(this.toList())

fun Sequence<Sample>.asInts() = this.map { it.asInt() }

fun Sequence<Window<Sample>>.asGroupedInts() = this.map { w -> w.elements.map { it.asInt() } }
fun Sequence<Window<Sample>>.asGroupedDoubles() = this.map { w -> w.elements }

class IntStream(
        val seq: List<Int>,
        val start: Long = 0,
        val end: Long? = null,
        val timeUnit: TimeUnit = TimeUnit.MILLISECONDS
) : BeanStream<Sample> {
    override fun asSequence(sampleRate: Float): Sequence<Sample> {
        val startIdx = timeToSampleIndexFloor(start, timeUnit, sampleRate).toInt()
        val endIdx = end?.let { timeToSampleIndexCeil(it, timeUnit, sampleRate).toInt() } ?: Int.MAX_VALUE
        return seq
                .drop(startIdx)
                .take(endIdx - startIdx)
                .asSequence().map { sampleOf(it) }
    }

    override fun inputs(): List<AnyBean> = throw UnsupportedOperationException()

    override val parameters: BeanParams
        get() = throw UnsupportedOperationException()

}

class DoubleStream(
        val seq: List<Double>,
        val start: Long = 0,
        val end: Long? = null,
        val timeUnit: TimeUnit = TimeUnit.MILLISECONDS
) : BeanStream<Sample> {
    override fun asSequence(sampleRate: Float): Sequence<Sample> {
        val startIdx = timeToSampleIndexFloor(start, timeUnit, sampleRate).toInt()
        val endIdx = end?.let { timeToSampleIndexCeil(it, timeUnit, sampleRate).toInt() } ?: Int.MAX_VALUE
        return seq
                .drop(startIdx)
                .take(endIdx - startIdx)
                .asSequence().map { sampleOf(it) }
    }

    override fun inputs(): List<AnyBean> = throw UnsupportedOperationException()

    override val parameters: BeanParams
        get() = throw UnsupportedOperationException()

}

fun <T> Assert<List<T>>.at(idx: Int): Assert<T> = this.prop("[$idx]") { it[idx] }

fun seqStream() = SeqInput()

class SeqInput constructor(
        val params: NoParams = NoParams()
) : StreamInput, SinglePartitionBean {

    private val seq = (0..10_000_000_000).asSequence().map { 1e-10 * it }

    override val parameters: BeanParams = params

    override fun asSequence(sampleRate: Float): Sequence<Sample> = seq


}
