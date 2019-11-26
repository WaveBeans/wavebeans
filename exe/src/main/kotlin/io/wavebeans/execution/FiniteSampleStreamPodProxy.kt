package io.wavebeans.execution

import io.wavebeans.lib.Sample
import io.wavebeans.lib.stream.FiniteSampleStream
import java.util.concurrent.TimeUnit

@ExperimentalStdlibApi
class FiniteSampleStreamPodProxy(
        pointedTo: PodKey,
        forPartition: Int
) : StreamingPodProxy<Sample, FiniteSampleStream>(
        pointedTo = pointedTo,
        forPartition = forPartition,
        converter = { it.nullableSampleList() }
), FiniteSampleStream {

    override fun length(timeUnit: TimeUnit): Long {
        val bush = podDiscovery.bushFor(pointedTo)
        val caller = bushCallerRepository.create(bush, pointedTo)

        return caller.call("length?timeUnit=${timeUnit.name}").get().long()
    }
}

class FiniteSampleStreamMergingPodProxy(
        override val readsFrom: List<PodKey>,
        forPartition: Int
) : MergingPodProxy<Sample, FiniteSampleStream>(
        forPartition = forPartition,
        converter = { it.nullableSampleList() }
), FiniteSampleStream {

    override fun length(timeUnit: TimeUnit): Long {
        TODO()
    }
}