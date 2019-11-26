package io.wavebeans.lib

import kotlin.math.round


const val INT_24BIT_MAX_VALUE = 8388608
const val DEFAULT_SAMPLE_ARRAY_SIZE = 512

/** Internal representation of sample. */
typealias Sample = Double

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
inline fun sampleOf(x: Double): Sample = /*if (x > 1.0 || x < -1.0) throw UnsupportedOperationException("x should belong to range [-1.0, 1.0] but it's $x") else */x

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
inline fun Byte.asUnsignedByte(): Int {
    val it = this.toInt() and 0xFF
    return it and 0x7F or (it.inv() and 0x80)
}
