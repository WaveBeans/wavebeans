package io.wavebeans.execution.podproxy

import io.wavebeans.execution.medium.FftSampleArray
import io.wavebeans.execution.pod.PodKey
import io.wavebeans.execution.medium.SampleArray
import io.wavebeans.execution.medium.nullableFftSampleArrayList
import io.wavebeans.execution.medium.nullableSampleArrayList
import io.wavebeans.lib.Sample
import io.wavebeans.lib.ZeroSample
import io.wavebeans.lib.stream.SampleStream
import io.wavebeans.lib.stream.fft.FftSample
import io.wavebeans.lib.stream.fft.FftStream
import io.wavebeans.lib.stream.fft.ZeroFftSample

class FftStreamPodProxy(
        podKey: PodKey,
        forPartition: Int
) : FftStream, StreamingPodProxy<FftSample, FftStream, FftSampleArray>(
        pointedTo = podKey,
        forPartition = forPartition,
        converter = { it.nullableFftSampleArrayList() },
        elementExtractor = { arr, i -> if (i < arr.size) arr[i] else null },
        zeroEl = { ZeroFftSample }
) {
    override fun estimateFftSamplesCount(samplesCount: Long): Long = throw UnsupportedOperationException("not required")
}

class FftStreamMergingPodProxy(
        override val readsFrom: List<PodKey>,
        forPartition: Int
) : MergingPodProxy<FftSample, FftStream, FftSampleArray>(
        forPartition = forPartition,
        converter = { it.nullableFftSampleArrayList() },
        elementExtractor = { arr, i -> if (i < arr.size) arr[i] else null },
        zeroEl = { ZeroFftSample }
), FftStream {
    override fun estimateFftSamplesCount(samplesCount: Long): Long = throw UnsupportedOperationException("not required")
}