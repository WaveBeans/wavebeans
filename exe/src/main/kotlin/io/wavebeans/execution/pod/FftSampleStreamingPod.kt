package io.wavebeans.execution.pod

import io.wavebeans.execution.medium.FftSampleArray
import io.wavebeans.execution.medium.SampleArray
import io.wavebeans.execution.medium.createFftSampleArray
import io.wavebeans.execution.medium.createSampleArray
import io.wavebeans.lib.BeanParams
import io.wavebeans.lib.BeanStream
import io.wavebeans.lib.Sample
import io.wavebeans.lib.stream.fft.FftSample
import io.wavebeans.lib.stream.fft.FftStream
import java.util.concurrent.TimeUnit

class FftSampleStreamingPod(
        bean: BeanStream<FftSample, *>,
        podKey: PodKey
) : StreamingPod<FftSample, FftSampleArray, BeanStream<FftSample, *>>(
        bean = bean,
        podKey = podKey,
        converter = { list ->
            val i = list.iterator()
            createFftSampleArray(list.size) { i.next() }
        }
)

class FftStreamingPod(
        bean: FftStream,
        podKey: PodKey
) : StreamingPod<FftSample, FftSampleArray, FftStream>(
        bean = bean,
        podKey = podKey,
        converter = { list ->
            val i = list.iterator()
            createFftSampleArray(list.size) { i.next() }
        }
), FftStream {
    override fun estimateFftSamplesCount(samplesCount: Long): Long = bean.estimateFftSamplesCount(samplesCount)

    override fun asSequence(sampleRate: Float): Sequence<FftSample> = throw UnsupportedOperationException("not required")

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): FftStream = throw UnsupportedOperationException("not required")

    override val parameters: BeanParams
        get() = throw UnsupportedOperationException("not required")

}

