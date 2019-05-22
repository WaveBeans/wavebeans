package mux.lib.stream

import kotlin.math.round

/** Internal representation of sample. */
typealias Sample = Double

const val ZeroSample: Sample = 0.0

@Suppress("NOTHING_TO_INLINE")
inline fun sampleOf(x: Long): Sample = sampleOf(x.toDouble() / Long.MAX_VALUE)

@Suppress("NOTHING_TO_INLINE")
inline fun sampleOf(x: Int): Sample = sampleOf(x.toDouble() / Int.MAX_VALUE)

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
inline fun Sample.asShort(): Short = round(this * Short.MAX_VALUE).toShort()

@Suppress("NOTHING_TO_INLINE")
inline fun Sample.asByte(): Byte = round(this * Byte.MAX_VALUE).toByte()

