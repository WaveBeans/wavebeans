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

data class AfterFillingFiniteStreamParams<T>(
        val zeroFiller: T
) : BeanParams

private class AfterFillingFiniteStream<T : Any>(
        override val input: FiniteStream<T>,
        override val parameters: AfterFillingFiniteStreamParams<T>
) : BeanStream<T>, SingleBean<T> {

    override val desiredSampleRate: Float? by lazy { input.desiredSampleRate }

    override fun asSequence(sampleRate: Float): Sequence<T> {
        return object : Iterator<T> {

            val iterator = input
                    .asSequence(sampleRate)
                    .iterator()

            override fun hasNext(): Boolean = true

            override fun next(): T {
                return if (iterator.hasNext()) { // there is something left to read
                    iterator.next()
                } else {
                    parameters.zeroFiller
                }
            }
        }.asSequence()
    }
}