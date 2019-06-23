package mux.lib.stream

import mux.lib.TimeRangeProjectable
import mux.lib.file.Informable
import mux.lib.samplesCountToLength
import java.util.concurrent.TimeUnit

interface SampleStream : Informable, TimeRangeProjectable<SampleStream> {

    /** number of samples in the stream. */
    fun samplesCount(): Int

    fun asSequence(sampleRate: Float): Sequence<Sample>

    fun mixStream(pos: Int, sampleStream: SampleStream): SampleStream {
        return MixedSampleStream(this, sampleStream, pos)
    }

    /**
     * The length of the stream with specified sample rate, the output units might be specified as parameter.
     * Effectively uses [samplesCount] and divides it by sample rate. Depending on [TimeUnit] may loose precision,
     * if precision is very important use [TimeUnit.NANOSECONDS].
     *
     * @param sampleRate to calculate length with that sample rate
     *
     */
    fun length(sampleRate: Float, timeUnit: TimeUnit = TimeUnit.MILLISECONDS): Long =
            samplesCountToLength(samplesCount().toLong(), sampleRate, timeUnit)
}

class SampleStreamException(message: String) : Exception(message)

