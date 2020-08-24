package io.wavebeans.lib.stream

import io.wavebeans.lib.*
import kotlinx.serialization.Serializable

class AfterFilling<T : Any>(
        private val zeroFiller: T
) : FiniteToStream<T> {
    override fun convert(finiteStream: FiniteStream<T>): BeanStream<T> {
        return AfterFillingFiniteStream(finiteStream, AfterFillingFiniteStreamParams(zeroFiller))
    }
}

@Serializable
data class AfterFillingFiniteStreamParams<T>(
        val zeroFiller: T
) : BeanParams()

private class AfterFillingFiniteStream<T : Any>(
        val finiteStream: FiniteStream<T>,
        val params: AfterFillingFiniteStreamParams<T>
) : BeanStream<T>, SingleBean<T> {

    override val parameters: BeanParams = params

    override val input: Bean<T> = finiteStream

    override fun asSequence(sampleRate: Float): Sequence<T> {
        return object : Iterator<T> {

            val iterator = finiteStream
                    .asSequence(sampleRate)
                    .iterator()

            override fun hasNext(): Boolean = true

            override fun next(): T {
                return if (iterator.hasNext()) { // there is something left to read
                    iterator.next()
                } else {
                    params.zeroFiller
                }
            }
        }.asSequence()
    }
}