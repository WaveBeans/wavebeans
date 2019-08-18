package mux.lib.stream

import mux.lib.BitDepth
import mux.lib.TimeRangeProjectable
import mux.lib.file.Informable
import mux.lib.io.ByteArrayLittleEndianAudioInput
import mux.lib.io.SineGeneratedInput
import mux.lib.samplesCountToLength
import java.util.concurrent.TimeUnit

interface SampleStream : Informable, TimeRangeProjectable<SampleStream> {

    /** number of samples in the stream. */
    fun samplesCount(): Int

    fun asSequence(sampleRate: Float): Sequence<Sample>

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

operator fun SampleStream.minus(d: SampleStream): SampleStream = diff(this, d)

operator fun SampleStream.plus(d: SampleStream): SampleStream = sum(this, d)

fun Iterable<Int>.sampleStream(sampleRate: Float, bitDepth: BitDepth = BitDepth.BIT_8): SampleStream {
    return AudioSampleStream(
            ByteArrayLittleEndianAudioInput(sampleRate, bitDepth, this.map {
                when (bitDepth) {
                    BitDepth.BIT_8 -> it.toByte()
                    BitDepth.BIT_16 -> TODO()
                    BitDepth.BIT_24 -> TODO()
                    BitDepth.BIT_32 -> TODO()
                    BitDepth.BIT_64 -> TODO()
                }
            }.toList().toByteArray())
    )
}

fun Number.sine(
        time: Number,
        sampleRate: Float,
        amplitude: Double = 1.0
): AudioSampleStream {
    return AudioSampleStream(SineGeneratedInput(sampleRate, this.toDouble(), amplitude, time.toDouble()))
}