package io.wavebeans.lib.stream

import io.wavebeans.lib.BeanStream
import io.wavebeans.lib.Sample

fun FiniteSampleStream.sampleStream(converter: FiniteToStream): BeanStream<Sample> = converter.convert(this)

interface FiniteToStream {

    fun convert(finiteSampleStream: FiniteSampleStream): BeanStream<Sample>
}
