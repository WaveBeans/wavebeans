package io.wavebeans.execution.podproxy

import io.wavebeans.execution.pod.PodKey
import io.wavebeans.execution.medium.*
import io.wavebeans.lib.stream.fft.FftSample
import io.wavebeans.lib.stream.fft.FiniteFftStream
import io.wavebeans.lib.stream.fft.ZeroFftSample
import java.util.concurrent.TimeUnit

@ExperimentalStdlibApi
class FiniteFftStreamPodProxy(
        pointedTo: PodKey,
        forPartition: Int
) : StreamingPodProxy<FftSample, FiniteFftStream, FftSampleArray>(
        pointedTo = pointedTo,
        forPartition = forPartition,
        converter = { it.nullableFftSampleArrayList() },
        elementExtractor = { arr, i -> if (i < arr.size) arr[i] else null },
        zeroEl = { ZeroFftSample }
), FiniteFftStream {

    override fun length(timeUnit: TimeUnit): Long {
        val bush = podDiscovery.bushFor(pointedTo)
        val caller = bushCallerRepository.create(bush, pointedTo)

        return caller.call("length?timeUnit=${timeUnit.name}").get().long()
    }
}

class FiniteFftStreamMergingPodProxy(
        override val readsFrom: List<PodKey>,
        forPartition: Int
) : MergingPodProxy<FftSample, FiniteFftStream, FftSampleArray>(
        forPartition = forPartition,
        converter = { it.nullableFftSampleArrayList() },
        elementExtractor = { arr, i -> if (i < arr.size) arr[i] else null },
        zeroEl = { ZeroFftSample }
), FiniteFftStream {

    override fun length(timeUnit: TimeUnit): Long {
        TODO()
    }
}