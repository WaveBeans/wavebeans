package io.wavebeans.lib.stream

import io.wavebeans.lib.*

class ZeroFilling : FiniteToStream<Sample> {
    override fun convert(finiteStream: FiniteStream<Sample>): BeanStream<Sample> {
        return ZeroFillingFiniteSampleStream(finiteStream)
    }
}

private class ZeroFillingFiniteSampleStream(
        val finiteStream: FiniteStream<Sample>,
        val params: NoParams = NoParams()
) : BeanStream<Sample>, SingleBean<Sample> {

    override val parameters: BeanParams = params

    override val input: Bean<Sample> = finiteStream

    override fun asSequence(sampleRate: Float): Sequence<Sample> {
        return object : Iterator<Sample> {

            val iterator = finiteStream
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