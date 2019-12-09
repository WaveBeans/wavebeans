package io.wavebeans.execution.podproxy

import io.wavebeans.execution.medium.FftSampleArray
import io.wavebeans.execution.medium.long
import io.wavebeans.execution.medium.nullableFftSampleArrayList
import io.wavebeans.execution.pod.PodKey
import io.wavebeans.lib.stream.fft.FftSample
import io.wavebeans.lib.stream.fft.FftStream
import java.util.concurrent.TimeUnit.MILLISECONDS

class FftStreamPodProxy(
        podKey: PodKey,
        forPartition: Int
) : FftStream, StreamingPodProxy<FftSample, FftStream, FftSampleArray>(
        pointedTo = podKey,
        forPartition = forPartition,
        converter = { it.nullableFftSampleArrayList() },
        elementExtractor = { arr, i -> if (i < arr.size) arr[i] else null }
) {
    override fun estimateFftSamplesCount(samplesCount: Long): Long {
        val podKey = pointedTo
        val bush = podDiscovery.bushFor(podKey)
        val caller = bushCallerRepository.create(bush, podKey)

        return caller.call("estimateFftSamplesCount?samplesCount=${samplesCount}").get(5000, MILLISECONDS).long()
    }
}

class FftStreamMergingPodProxy(
        override val readsFrom: List<PodKey>,
        forPartition: Int
) : MergingPodProxy<FftSample, FftStream, FftSampleArray>(
        forPartition = forPartition,
        converter = { it.nullableFftSampleArrayList() },
        elementExtractor = { arr, i -> if (i < arr.size) arr[i] else null }
), FftStream {
    override fun estimateFftSamplesCount(samplesCount: Long): Long {
        val podKey = readsFrom.first()
        val bush = podDiscovery.bushFor(podKey)
        val caller = bushCallerRepository.create(bush, podKey)

        return caller.call("estimateFftSamplesCount?samplesCount=${samplesCount}").get(5000, MILLISECONDS).long()
    }
}