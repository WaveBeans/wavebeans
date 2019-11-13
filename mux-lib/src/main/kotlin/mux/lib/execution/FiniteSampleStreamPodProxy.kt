package mux.lib.execution

import mux.lib.*
import mux.lib.stream.FiniteSampleStream
import java.util.concurrent.TimeUnit

@ExperimentalStdlibApi
class FiniteSampleStreamPodProxy(
        podKey: PodKey,
        forPartition: Int
) : StreamingPodProxy<SampleArray, FiniteSampleStream>(
        pointedTo = podKey,
        forPartition = forPartition,
        converter = { it.nullableSampleArrayList() }
), FiniteSampleStream {

    override fun length(timeUnit: TimeUnit): Long {
        val bush = podDiscovery.bushFor(pointedTo)
        val caller = bushCallerRepository.create(bush, pointedTo)

        return caller.call("length?timeUnit=${timeUnit.name}").long()
    }
}

class FiniteSampleStreamMergingPodProxy(
        override val readsFrom: List<PodKey>,
        forPartition: Int
) : MergingPodProxy<SampleArray, FiniteSampleStream>(
        forPartition = forPartition,
        converter = { it.nullableSampleArrayList() }
), FiniteSampleStream {

    override fun length(timeUnit: TimeUnit): Long {
        TODO()
    }
}