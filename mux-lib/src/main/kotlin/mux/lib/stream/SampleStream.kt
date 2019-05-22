package mux.lib.stream

import mux.lib.file.Informable

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

    fun mixStream(pos: Int, sampleStream: SampleStream): SampleStream {
        return MixedSampleStream(this, sampleStream, pos)
    }
}

class SampleStreamException(message: String) : Exception(message)

