package mux.lib.stream

import mux.lib.BeanStream
import mux.lib.SampleArray

interface SampleStream : BeanStream<SampleArray, SampleStream>

class SampleStreamException(message: String) : Exception(message)
