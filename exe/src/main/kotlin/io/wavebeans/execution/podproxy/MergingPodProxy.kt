package io.wavebeans.execution.podproxy

import io.wavebeans.execution.*
import io.wavebeans.execution.config.ExecutionConfig
import io.wavebeans.execution.pod.PodKey
import io.wavebeans.lib.AnyBean
import io.wavebeans.lib.BeanParams
import io.wavebeans.lib.BeanStream

abstract class MergingPodProxy(
        override val forPartition: Int,
        val podDiscovery: PodDiscovery = PodDiscovery.default,
        val bushCallerRepository: BushCallerRepository = BushCallerRepository.default(podDiscovery),
        val prefetchBucketAmount: Int = ExecutionConfig.prefetchBucketAmount,
        val partitionSize: Int = ExecutionConfig.partitionSize
) : BeanStream<Any>, PodProxy {

    abstract val readsFrom: List<PodKey>

    override fun asSequence(sampleRate: Float): Sequence<Any> {
        return object : Iterator<Any> {

            val partitionIterators = readsFrom
                    .map {
                        PodProxyIterator(
                                sampleRate = sampleRate,
                                pod = it,
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

            override fun next(): Any {
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