package mux.lib.stream

import mux.lib.BeanStream
import mux.lib.Sample

interface SampleStream : BeanStream<Sample, SampleStream>

class SampleStreamException(message: String) : Exception(message)
