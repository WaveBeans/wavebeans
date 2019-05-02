package mux.lib.stream

import mux.lib.io.AudioInput
import java.lang.Math.max
import java.lang.Math.min

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
) {

    abstract fun rangeProjection(sampleStartIdx: Int, sampleEndIdx: Int): SampleStream


    /** number of samples in the stream. */
    abstract fun samplesCount(): Int

    /** samplesCount of the stream in milliseconds. */
    fun length(): Int = (samplesCount() / (sampleRate / 1000.0f)).toInt()

    fun downSample(ratio: Int): SampleStream = RatioDownSampledStream(this, ratio)

    abstract fun asSequence(): Sequence<Sample>
}

class RatioDownSampledStream(
        private val sourceStream: SampleStream,
        private val ratio: Int,
        sourceStartIdx: Int? = null,
        sourceEndIdx: Int? = null

) : SampleStream(sourceStream.sampleRate / ratio) {

    init {
        if (sourceStartIdx != null && sourceEndIdx ?: Int.MAX_VALUE <= sourceStartIdx)
            throw IllegalArgumentException("sourceStartIdx[$sourceStartIdx] should be less then sourceEndIdx[$sourceEndIdx]")
    }

    private val stream = if (sourceStartIdx != null) {
        sourceStream.rangeProjection(sourceStartIdx, sourceEndIdx ?: sourceStream.samplesCount())
    } else {
        sourceStream
    }

    override fun samplesCount(): Int = stream.samplesCount() / ratio

    override fun asSequence(): Sequence<Sample> {
        return stream.asSequence()
                .filterIndexed { index, _ -> index % ratio == 0 }

    }


    override fun rangeProjection(sampleStartIdx: Int, sampleEndIdx: Int): SampleStream {
        val sourceStartIdx = sampleStartIdx * ratio
        val sourceEndIdx = sampleEndIdx * ratio

        return RatioDownSampledStream(sourceStream, ratio, sourceStartIdx, sourceEndIdx)
    }
}


class AudioSampleStream(private val input: AudioInput, sampleRate: Float) : SampleStream(sampleRate) {

    override fun samplesCount(): Int = input.size()

    override fun asSequence(): Sequence<Sample> = input.asSequence()

    override fun rangeProjection(sampleStartIdx: Int, sampleEndIdx: Int): SampleStream {
        val s = max(sampleStartIdx, 0)
        val e = min(sampleEndIdx, input.size())

        return AudioSampleStream(input.subInput(s, e - s), sampleRate)
    }
}
