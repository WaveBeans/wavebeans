package io.wavebeans.execution.podproxy

import io.wavebeans.execution.*
import io.wavebeans.execution.config.ExecutionConfig
import io.wavebeans.execution.pod.PodKey
import io.wavebeans.lib.AnyBean
import io.wavebeans.lib.BeanParams
import io.wavebeans.lib.stream.FiniteStream
import java.util.concurrent.TimeUnit

abstract class MergingPodProxy(
        override val forPartition: Int,
        val podDiscovery: PodDiscovery = PodDiscovery.default,
        val bushCallerRepository: BushCallerRepository = BushCallerRepository.default(podDiscovery),
        val prefetchBucketAmount: Int = ExecutionConfig.prefetchBucketAmount,
        val partitionSize: Int = ExecutionConfig.partitionSize
) : FiniteStream<Any>, PodProxy {

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

    override val desiredSampleRate: Float? by lazy {
        readsFrom.map { pod ->
            val bush = podDiscovery.bushFor(pod)
            val caller = bushCallerRepository.create(bush, pod)
            caller.call("desiredSampleRate").get(5000, TimeUnit.MILLISECONDS).obj as Float?
        }.distinct().let {
            require(it.size == 1) { "Desired sample rate from pods $readsFrom is ambiguous: $it. Something requires resampling first." }
            it.first()
        }
    }

    override fun inputs(): List<AnyBean> = throw UnsupportedOperationException("Not required")

    override val parameters: BeanParams
        get() = throw UnsupportedOperationException("Not required")

    override fun toString(): String {
        return "${this::class.simpleName}->$readsFrom for $forPartition"
    }

    override fun length(timeUnit: TimeUnit): Long {
        throw UnsupportedOperationException("Not a finite pod proxy")
    }

    override fun samplesCount(): Long {
        throw UnsupportedOperationException("Not a finite pod proxy")
    }
}