package io.wavebeans.lib.stream

import io.wavebeans.lib.*
import kotlinx.serialization.Serializable
import java.util.concurrent.TimeUnit

class ZeroFilling : FiniteToStream {
    override fun convert(finiteSampleStream: FiniteSampleStream): BeanStream<Sample> {
        return ZeroFillingFiniteSampleStream(finiteSampleStream)
    }
}

private class ZeroFillingFiniteSampleStream(
        val finiteSampleStream: FiniteSampleStream,
        val params: NoParams = NoParams()
) : BeanStream<Sample>, SingleBean<Sample> {

    override val parameters: BeanParams = params

    override val input: Bean<Sample> = finiteSampleStream

    override fun asSequence(sampleRate: Float): Sequence<Sample> {
        return object : Iterator<Sample> {

            val iterator = finiteSampleStream
                    .asSequence(sampleRate)
                    .iterator()


            override fun hasNext(): Boolean = true

            override fun next(): Sample {
                return if (iterator.hasNext()) { // there is something left to read
                    iterator.next()
                } else {
                    ZeroSample
                }
            }
        }.asSequence()
    }
}