package io.wavebeans.lib.stream

fun FiniteSampleStream.sampleStream(converter: FiniteToStream): SampleStream = converter.convert(this)

interface FiniteToStream {

    fun convert(finiteSampleStream: FiniteSampleStream): SampleStream
}
