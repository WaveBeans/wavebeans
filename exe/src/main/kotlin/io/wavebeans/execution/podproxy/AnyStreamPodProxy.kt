package io.wavebeans.execution.podproxy

import io.wavebeans.execution.medium.value
import io.wavebeans.execution.pod.PodKey
import io.wavebeans.lib.BeanStream
import io.wavebeans.lib.stream.FiniteStream
import java.util.concurrent.TimeUnit

class AnyStreamPodProxy(
        podKey: PodKey,
        forPartition: Int
) : BeanStream<Any>, StreamingPodProxy(
        pointedTo = podKey,
        forPartition = forPartition
)

class AnyStreamMergingPodProxy(
        override val readsFrom: List<PodKey>,
        forPartition: Int
) : MergingPodProxy(
        forPartition = forPartition
), BeanStream<Any>

class AnyFiniteStreamMergingPodProxy(
        override val readsFrom: List<PodKey>,
        forPartition: Int
) : MergingPodProxy(
        forPartition = forPartition
), FiniteStream<Any> {
    override fun length(timeUnit: TimeUnit): Long {
        return readsFrom.map { pointedTo ->
            val bush = podDiscovery.bushFor(pointedTo)
            val caller = bushCallerRepository.create(bush, pointedTo)
            caller.call("length?timeUnit=${timeUnit}")
                    .get(5000, TimeUnit.MILLISECONDS)
                    .value<Long>()

        }.maxOrNull()!!
    }
}
