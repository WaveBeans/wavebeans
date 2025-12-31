package io.wavebeans.lib.stream

import io.wavebeans.lib.Bean
import io.wavebeans.lib.BeanStream
import io.wavebeans.lib.MultiBean
import io.wavebeans.lib.NoParams
import io.wavebeans.lib.SinglePartitionBean
import io.wavebeans.lib.TimeUnit

operator fun <T : Any> FiniteStream<T>.rangeTo(finiteStream: FiniteStream<T>): FiniteStream<T> =
        ConcatenatedFiniteStream(this, finiteStream)

operator fun <T : Any> FiniteStream<T>.rangeTo(stream: BeanStream<T>): BeanStream<T> =
        ConcatenatedStream(this, stream)

class ConcatenatedFiniteStream<T : Any>(
        private val stream1: FiniteStream<T>,
        private val stream2: FiniteStream<T>,
        override val parameters: NoParams = NoParams()
) : AbstractMultiOperationBeanStream<T>(listOf(stream1, stream2)), FiniteStream<T>, MultiBean<T>, SinglePartitionBean {

    override val inputs: List<Bean<T>> = listOf(stream1, stream2)

    @Suppress("UNCHECKED_CAST")
    override fun operationSequence(inputs: List<Sequence<Any>>, sampleRate: Float): Sequence<T> {
        val stream1 = inputs.first()
        val stream2 = inputs.drop(1).first()
        return (stream1 + stream2).map { it as T }
    }

    override fun length(timeUnit: TimeUnit): Long {
        return stream1.length(timeUnit) + stream2.length(timeUnit)
    }

    override fun samplesCount(): Long = stream1.samplesCount() + stream2.samplesCount()
}

class ConcatenatedStream<T : Any>(
        private val stream1: FiniteStream<T>,
        private val stream2: BeanStream<T>,
        override val parameters: NoParams = NoParams()
) : AbstractMultiOperationBeanStream<T>(listOf(stream1, stream2)), BeanStream<T>, MultiBean<T>, SinglePartitionBean {

    override val inputs: List<Bean<T>> = listOf(stream1, stream2)

    @Suppress("UNCHECKED_CAST")
    override fun operationSequence(inputs: List<Sequence<Any>>, sampleRate: Float): Sequence<T> {
        val stream1 = inputs.first()
        val stream2 = inputs.drop(1).first()
        return (stream1 + stream2).map { it as T }
    }
}