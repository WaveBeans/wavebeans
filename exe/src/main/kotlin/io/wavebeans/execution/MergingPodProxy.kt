package io.wavebeans.execution

import io.wavebeans.lib.Bean
import io.wavebeans.lib.BeanParams
import io.wavebeans.lib.BeanStream
import java.util.concurrent.TimeUnit

abstract class MergingPodProxy<T : Any, S : Any, ARRAY_T>(
        val converter: (PodCallResult) -> List<ARRAY_T>?,
        val elementExtractor: (ARRAY_T, Int) -> T?,
        val zeroEl: () -> T,
        override val forPartition: Int,
        val podDiscovery: PodDiscovery = PodDiscovery.default,
        val bushCallerRepository: BushCallerRepository = BushCallerRepository.default(podDiscovery),
        val prefetchBucketAmount: Int = DEFAULT_PREFETCH_BUCKET_AMOUNT,
        val partitionSize: Int = DEFAULT_PARTITION_SIZE
) : BeanStream<T, S>, PodProxy<T, S> {

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
                                zeroEl = zeroEl,
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

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): S {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun inputs(): List<Bean<*, *>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override val parameters: BeanParams
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override fun toString(): String {
        return "${this::class.simpleName}->$readsFrom for $forPartition"
    }
}