package mux.lib.stream

import mux.lib.MuxStream
import mux.lib.Sample
import mux.lib.io.SineGeneratedInput

interface SampleStream : MuxStream<Sample, SampleStream>

class SampleStreamException(message: String) : Exception(message)

operator fun SampleStream.minus(d: SampleStream): SampleStream = diff(this, d)

operator fun SampleStream.plus(d: SampleStream): SampleStream = sum(this, d)