package io.wavebeans.execution.podproxy

import io.wavebeans.execution.medium.value
import io.wavebeans.execution.pod.PodKey
import io.wavebeans.lib.stream.FiniteStream
import java.util.concurrent.TimeUnit

class AnyFiniteStreamPodProxy(
        podKey: PodKey,
        forPartition: Int
) : FiniteStream<Any>, StreamingPodProxy(
        pointedTo = podKey,
        forPartition = forPartition
) {
    override fun length(timeUnit: TimeUnit): Long {
        val bush = podDiscovery.bushFor(pointedTo)
        val caller = bushCallerRepository.create(bush, pointedTo)
        return caller.call("length?timeUnit=${timeUnit}")
                .get(5000, TimeUnit.MILLISECONDS)
                .value()
    }
}