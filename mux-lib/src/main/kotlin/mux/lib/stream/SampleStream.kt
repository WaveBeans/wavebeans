package mux.lib.stream

import mux.lib.file.Informable

interface SampleStream : Informable {

    fun rangeProjection(sampleStartIdx: Int, sampleEndIdx: Int): SampleStream

    /** number of samples in the stream. */
    fun samplesCount(): Int

    fun asSequence(sampleRate: Float): Sequence<Sample>

    fun mixStream(pos: Int, sampleStream: SampleStream): SampleStream {
        return MixedSampleStream(this, sampleStream, pos)
    }
}

class SampleStreamException(message: String) : Exception(message)

