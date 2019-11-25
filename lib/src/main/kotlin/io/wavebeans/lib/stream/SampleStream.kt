package io.wavebeans.lib.stream

import io.wavebeans.lib.BeanStream
import io.wavebeans.lib.SampleArray

interface SampleStream : BeanStream<SampleArray, SampleStream>

class SampleStreamException(message: String) : Exception(message)
