package io.wavebeans.execution.podproxy

import io.wavebeans.execution.*
import io.wavebeans.execution.medium.PodCallResult
import io.wavebeans.execution.pod.DEFAULT_PARTITION_SIZE
import io.wavebeans.execution.pod.PodKey
import io.wavebeans.lib.AnyBean
import io.wavebeans.lib.Bean
import io.wavebeans.lib.BeanParams
import io.wavebeans.lib.BeanStream
import java.util.concurrent.TimeUnit

abstract class MergingPodProxy<T : Any, ARRAY_T>(
        val converter: (PodCallResult) -> List<ARRAY_T>?,
        val elementExtractor: (ARRAY_T, Int) -> T?,
        override val forPartition: Int,
        val podDiscovery: PodDiscovery = PodDiscovery.default,
        val bushCallerRepository: BushCallerRepository = BushCallerRepository.default(podDiscovery),
        val prefetchBucketAmount: Int = DEFAULT_PREFETCH_BUCKET_AMOUNT,
        val partitionSize: Int = DEFAULT_PARTITION_SIZE
) : BeanStream<T>, PodProxy<T> {

    abstract val readsFrom: List<PodKey>

    override fun asSequence(sampleRate: Float): Sequence<T> {
        return object : Iterator<T> {

            val partitionIterators = readsFrom
                    .map {
                        PodProxyIterator(
                                sampleRate = sampleRate,
                                pod = it,
                                converter = converter,
                                elementExtractor = elementExtractor,
                                readingPartition = forPartition,
                                podDiscovery = podDiscovery,
                                bushCallerRepository = bushCallerRepository,
                                prefetchBucketAmount = prefetchBucketAmount,
                                partitionSize = partitionSize
                        )
                    }.toTypedArray()

            var activePartitionIterator = 0
            var partitionPointer = 0

            override fun hasNext(): Boolean {
                return partitionIterators[activePartitionIterator].hasNext()
            }

            override fun next(): T {
                val el = partitionIterators[activePartitionIterator].next()
                if (++partitionPointer >= partitionSize) {
                    activePartitionIterator = (activePartitionIterator + 1) % partitionIterators.size
                    partitionPointer = 0
                }
                return el
            }

        }.asSequence()
    }

    override fun inputs(): List<AnyBean> = throw UnsupportedOperationException("Not required")

    override val parameters: BeanParams
        get() = throw UnsupportedOperationException("Not required")

    override fun toString(): String {
        return "${this::class.simpleName}->$readsFrom for $forPartition"
    }
}