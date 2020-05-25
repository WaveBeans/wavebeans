package io.wavebeans.lib

import io.wavebeans.lib.stream.window.Window
import kotlin.math.round

const val INT_24BIT_MAX_VALUE = 8388608

/** Internal representation of sample. */
typealias Sample = Double

typealias SampleArray = DoubleArray

const val ZeroSample: Sample = 0.0

@Suppress("NOTHING_TO_INLINE")
inline fun sampleOf(x: Long): Sample = sampleOf(x.toDouble() / Long.MAX_VALUE)

@Suppress("NOTHING_TO_INLINE")
inline fun sampleOf(x: Int, as24bit: Boolean = false): Sample =
        if (as24bit)
            sampleOf(x.toDouble() / INT_24BIT_MAX_VALUE)
        else
            sampleOf(x.toDouble() / Int.MAX_VALUE)

@Suppress("NOTHING_TO_INLINE")
inline fun sampleOf(x: Short): Sample = sampleOf(x.toDouble() / Short.MAX_VALUE)

@Suppress("NOTHING_TO_INLINE")
inline fun sampleOf(x: Byte): Sample = sampleOf(x.toDouble() / Byte.MAX_VALUE)

@Suppress("NOTHING_TO_INLINE")
inline fun sampleOf(x: Double): Sample = x

@Suppress("NOTHING_TO_INLINE")
inline fun sampleOf(x: Float): Sample = x.toDouble()

@Suppress("NOTHING_TO_INLINE")
inline fun Sample.asLong(): Long = round(this * Long.MAX_VALUE).toLong()

@Suppress("NOTHING_TO_INLINE")
inline fun Sample.asInt(): Int = round(this * Int.MAX_VALUE).toInt()

@Suppress("NOTHING_TO_INLINE")
inline fun Sample.as24BitInt(): Int {
    return round(this * INT_24BIT_MAX_VALUE).toInt()
}

@Suppress("NOTHING_TO_INLINE")
inline fun Sample.asShort(): Short = round(this * Short.MAX_VALUE).toShort()

@Suppress("NOTHING_TO_INLINE")
inline fun Sample.asByte(): Byte = round(this * Byte.MAX_VALUE).toByte()

@Suppress("NOTHING_TO_INLINE")
inline fun Sample.asDouble(): Double = this

@Suppress("NOTHING_TO_INLINE")
inline fun Byte.asUnsignedByte(): Int {
    val it = this.toInt() and 0xFF
    return it and 0x7F or (it.inv() and 0x80)
}

@Suppress("NOTHING_TO_INLINE")
fun sampleArrayOf(list: List<Sample>): SampleArray {
    val i = list.iterator()
    return SampleArray(list.size) { i.next() }
}

@Suppress("NOTHING_TO_INLINE")
fun sampleArrayOf(vararg sample: Sample): SampleArray = SampleArray(sample.size) { sample[it] }

@Suppress("NOTHING_TO_INLINE")
fun sampleArrayOf(window: Window<Sample>): SampleArray = sampleArrayOf(window.elements)

operator fun Sample?.plus(other: Sample?): Sample = (this ?: ZeroSample) + (other ?: ZeroSample)
operator fun Sample?.plus(other: Sample): Sample = (this ?: ZeroSample) + other
operator fun Sample?.minus(other: Sample?): Sample = (this ?: ZeroSample) - (other ?: ZeroSample)
operator fun Sample?.minus(other: Sample): Sample = (this ?: ZeroSample) - other
operator fun Sample?.times(other: Sample?): Sample = (this ?: ZeroSample) * (other ?: ZeroSample)
operator fun Sample?.times(other: Sample): Sample = (this ?: ZeroSample) * other
operator fun Sample?.div(other: Sample): Sample = (this ?: ZeroSample) / other