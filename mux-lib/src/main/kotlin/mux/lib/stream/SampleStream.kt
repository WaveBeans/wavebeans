package mux.lib.stream

import mux.lib.Sample
import mux.lib.TimeRangeProjectable
import mux.lib.io.SineGeneratedInput

interface SampleStream : MuxStream<Sample>, TimeRangeProjectable<SampleStream>

class SampleStreamException(message: String) : Exception(message)

operator fun SampleStream.minus(d: SampleStream): SampleStream = diff(this, d)

operator fun SampleStream.plus(d: SampleStream): SampleStream = sum(this, d)

fun Number.sine(
        amplitude: Double = 1.0,
        timeOffset: Double = 0.0
): SampleStream {
    return SineGeneratedInput(this.toDouble(), amplitude, timeOffset).sampleStream()
}