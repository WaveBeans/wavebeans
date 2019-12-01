package io.wavebeans.execution.podproxy

import io.wavebeans.execution.*
import io.wavebeans.execution.medium.PodCallResult
import io.wavebeans.execution.pod.DEFAULT_PARTITION_SIZE
import io.wavebeans.execution.pod.PodKey
import io.wavebeans.lib.Bean
import io.wavebeans.lib.BeanParams
import io.wavebeans.lib.BeanStream
import java.util.concurrent.TimeUnit

abstract class StreamingPodProxy<T : Any, S : Any, ARRAY_T>(
        val pointedTo: PodKey,
        override val forPartition: Int,
        val podDiscovery: PodDiscovery = PodDiscovery.default,
        val bushCallerRepository: BushCallerRepository = BushCallerRepository.default(podDiscovery),
        val converter: (PodCallResult) -> List<ARRAY_T>?,
        val elementExtractor: (ARRAY_T, Int) -> T?,
        val zeroEl: () -> T,
        val prefetchBucketAmount: Int = DEFAULT_PREFETCH_BUCKET_AMOUNT,
        val partitionSize: Int = DEFAULT_PARTITION_SIZE
) : BeanStream<T, S>, PodProxy<T, S> {

    override fun asSequence(sampleRate: Float): Sequence<T> {
        return PodProxyIterator(
                sampleRate,
                pointedTo,
                forPartition,
                podDiscovery,
                bushCallerRepository,
                converter,
                elementExtractor,
                zeroEl,
                prefetchBucketAmount,
                partitionSize
        ).asSequence()
    }

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): S = throw UnsupportedOperationException("That's not required for PodProxy")

    override fun inputs(): List<Bean<*, *>> = throw UnsupportedOperationException("That's not required for PodProxy")

    override val parameters: BeanParams
        get() = throw UnsupportedOperationException("That's not required for PodProxy")

    override fun toString(): String = "${this::class.simpleName}->[$pointedTo] for $forPartition"
}