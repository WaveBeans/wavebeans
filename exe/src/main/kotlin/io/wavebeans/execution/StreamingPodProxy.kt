package io.wavebeans.execution

import io.wavebeans.lib.Bean
import io.wavebeans.lib.BeanParams
import io.wavebeans.lib.BeanStream
import java.util.concurrent.TimeUnit

abstract class StreamingPodProxy<T : Any, S : Any>(
        val pointedTo: PodKey,
        override val forPartition: Int,
        val podDiscovery: PodDiscovery = PodDiscovery.default,
        val bushCallerRepository: BushCallerRepository = BushCallerRepository.default(podDiscovery),
        val timeToReadAtOnce: Int = 10,
        val timeUnit: TimeUnit = TimeUnit.MILLISECONDS,
        val converter: (PodCallResult) -> List<T>?
) : BeanStream<T, S>, PodProxy<T, S> {

    override fun asSequence(sampleRate: Float): Sequence<T> {
        return PodProxyIterator(sampleRate, pointedTo, forPartition, podDiscovery, bushCallerRepository, converter, timeToReadAtOnce).asSequence()
    }

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): S = throw UnsupportedOperationException("That's not required for PodProxy")

    override fun inputs(): List<Bean<*, *>> = throw UnsupportedOperationException("That's not required for PodProxy")

    override val parameters: BeanParams
        get() = throw UnsupportedOperationException("That's not required for PodProxy")

    override fun toString(): String = "${this::class.simpleName}->[$pointedTo] for $forPartition"
}