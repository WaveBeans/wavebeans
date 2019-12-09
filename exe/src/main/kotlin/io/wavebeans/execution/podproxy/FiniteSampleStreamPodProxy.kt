package io.wavebeans.execution.podproxy

import io.wavebeans.execution.pod.PodKey
import io.wavebeans.execution.medium.SampleArray
import io.wavebeans.execution.medium.long
import io.wavebeans.execution.medium.nullableSampleArrayList
import io.wavebeans.lib.Sample
import io.wavebeans.lib.ZeroSample
import io.wavebeans.lib.stream.FiniteSampleStream
import java.util.concurrent.TimeUnit

@ExperimentalStdlibApi
class FiniteSampleStreamPodProxy(
        pointedTo: PodKey,
        forPartition: Int
) : StreamingPodProxy<Sample, FiniteSampleStream, SampleArray>(
        pointedTo = pointedTo,
        forPartition = forPartition,
        converter = { it.nullableSampleArrayList() },
        elementExtractor = { arr, i -> if (i < arr.size) arr[i] else null }
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
) : MergingPodProxy<Sample, FiniteSampleStream, SampleArray>(
        forPartition = forPartition,
        converter = { it.nullableSampleArrayList() },
        elementExtractor = { arr, i -> if (i < arr.size) arr[i] else null }
), FiniteSampleStream {

    override fun length(timeUnit: TimeUnit): Long {
        TODO()
    }
}