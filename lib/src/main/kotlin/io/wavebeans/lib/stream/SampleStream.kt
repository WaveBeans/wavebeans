package io.wavebeans.lib.stream

import io.wavebeans.lib.BeanStream
import io.wavebeans.lib.Sample

interface SampleStream : BeanStream<Sample, SampleStream>

class SampleStreamException(message: String) : Exception(message)
