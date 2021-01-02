package io.wavebeans.execution.podproxy

import io.wavebeans.execution.*
import io.wavebeans.execution.config.ExecutionConfig
import io.wavebeans.execution.pod.PodKey
import io.wavebeans.lib.AnyBean
import io.wavebeans.lib.BeanParams
import io.wavebeans.lib.BeanStream
import java.util.concurrent.TimeUnit

abstract class StreamingPodProxy(
        val pointedTo: PodKey,
        override val forPartition: Int,
        val podDiscovery: PodDiscovery = PodDiscovery.default,
        val bushCallerRepository: BushCallerRepository = BushCallerRepository.default(podDiscovery),
        val prefetchBucketAmount: Int = ExecutionConfig.prefetchBucketAmount,
        val partitionSize: Int = ExecutionConfig.partitionSize
) : BeanStream<Any>, PodProxy {

    override fun asSequence(sampleRate: Float): Sequence<Any> {
        return PodProxyIterator(
                sampleRate,
                pointedTo,
                forPartition,
                podDiscovery,
                bushCallerRepository,
                prefetchBucketAmount,
                partitionSize
        ).asSequence()
    }

    override fun inputs(): List<AnyBean> = throw UnsupportedOperationException("That's not required for PodProxy")

    override val desiredSampleRate: Float? by lazy {
        val bush = podDiscovery.bushFor(pointedTo)
        val caller = bushCallerRepository.create(bush, pointedTo)
        caller.call("desiredSampleRate").get(5000, TimeUnit.MILLISECONDS).obj as Float?
    }

    override val parameters: BeanParams
        get() = throw UnsupportedOperationException("That's not required for PodProxy")

    override fun toString(): String = "${this::class.simpleName}->[$pointedTo] for partition=$forPartition"
}