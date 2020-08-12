package io.wavebeans.lib.stream

import io.wavebeans.lib.*
import java.util.concurrent.TimeUnit

operator fun <T : Any> FiniteStream<T>.rangeTo(finiteStream: FiniteStream<T>): FiniteStream<T> =
        ConcatenatedFiniteStream(this, finiteStream)

operator fun <T : Any> FiniteStream<T>.rangeTo(stream: BeanStream<T>): BeanStream<T> =
        ConcatenatedStream(this, stream)

class ConcatenatedFiniteStream<T : Any>(
        private val stream1: FiniteStream<T>,
        private val stream2: FiniteStream<T>,
        override val parameters: NoParams = NoParams()
) : FiniteStream<T>, MultiBean<T>, SinglePartitionBean {

    override val inputs: List<Bean<T>>
        get() = listOf(stream1, stream2)

    override fun asSequence(sampleRate: Float): Sequence<T> {
        return stream1.asSequence(sampleRate) + stream2.asSequence(sampleRate)
    }

    override fun length(timeUnit: TimeUnit): Long {
        return stream1.length(timeUnit) + stream2.length(timeUnit)
    }
}

class ConcatenatedStream<T : Any>(
        private val stream1: FiniteStream<T>,
        private val stream2: BeanStream<T>,
        override val parameters: NoParams = NoParams()
) : BeanStream<T>, MultiBean<T>, SinglePartitionBean {

    override val inputs: List<Bean<T>>
        get() = listOf(stream1, stream2)

    override fun asSequence(sampleRate: Float): Sequence<T> {
        return stream1.asSequence(sampleRate) + stream2.asSequence(sampleRate)
    }
}