package mux.lib.stream

import mux.lib.file.Informable

/** Internal representation of sample. */
typealias Sample = Double

@Suppress("NOTHING_TO_INLINE")
inline fun sampleOf(x: Long): Sample = sampleOf(x.toDouble() / Long.MAX_VALUE)

@Suppress("NOTHING_TO_INLINE")
inline fun sampleOf(x: Int): Sample = sampleOf(x.toDouble() / Int.MAX_VALUE)

@Suppress("NOTHING_TO_INLINE")
inline fun sampleOf(x: Short): Sample = sampleOf(x.toDouble() / Short.MAX_VALUE)

@Suppress("NOTHING_TO_INLINE")
inline fun sampleOf(x: Byte): Sample = sampleOf(x.toDouble() / Byte.MAX_VALUE)

@Suppress("NOTHING_TO_INLINE")
inline fun sampleOf(x: Double): Sample = if (x > 1.0 || x < -1.0) throw UnsupportedOperationException("x should belong to range [-1.0, 1.0] but it's $x") else x

@Suppress("NOTHING_TO_INLINE")
inline fun Sample.asLong(): Long = (this * Long.MAX_VALUE).toLong()

@Suppress("NOTHING_TO_INLINE")
inline fun Sample.asInt(): Int = (this * Int.MAX_VALUE).toInt()

@Suppress("NOTHING_TO_INLINE")
inline fun Sample.asShort(): Short = (this * Short.MAX_VALUE).toShort()

@Suppress("NOTHING_TO_INLINE")
inline fun Sample.asByte(): Byte = (this * Byte.MAX_VALUE).toByte()

abstract class SampleStream(
        val sampleRate: Float
) : Informable {

    abstract fun rangeProjection(sampleStartIdx: Int, sampleEndIdx: Int): SampleStream

    /** number of samples in the stream. */
    abstract fun samplesCount(): Int

    /** length of the stream in milliseconds. */
    fun length(): Int = (samplesCount() / (sampleRate / 1000.0f)).toInt()

    fun downSample(ratio: Int): SampleStream = RatioDownSampledStream(this, ratio)

    abstract fun asSequence(): Sequence<Sample>
}


