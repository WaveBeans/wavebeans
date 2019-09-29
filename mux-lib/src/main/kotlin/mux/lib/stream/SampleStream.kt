package mux.lib.stream

import mux.lib.MuxStream
import mux.lib.Sample
import mux.lib.io.SineGeneratedInput

interface SampleStream : MuxStream<Sample, SampleStream>

class SampleStreamException(message: String) : Exception(message)
