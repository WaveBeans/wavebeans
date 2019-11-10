package mux.lib.execution

import mux.lib.*
import mux.lib.stream.SampleStream
import java.util.concurrent.TimeUnit

class SampleStreamStreamingPod(
        val bean: SampleStream,
        podKey: PodKey
) : StreamingPod<SampleArray, SampleStream>(podKey), SampleStream {

    override val parameters: BeanParams = NoParams()

    override fun asSequence(sampleRate: Float): Sequence<SampleArray> = bean.asSequence(sampleRate)

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): SampleStream {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun inputs(): List<Bean<*, *>> = listOf(bean)
}

class SampleStreamSplittingPod(
        val bean: SampleStream,
        podKey: PodKey,
        partitionCount: Int
) : SplittingPod<SampleArray, SampleStream>(podKey, partitionCount), SampleStream {

    override val parameters: BeanParams = NoParams()

    override fun asSequence(sampleRate: Float): Sequence<SampleArray> = bean.asSequence(sampleRate)

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): SampleStream {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun inputs(): List<Bean<*, *>> = listOf(bean)
}

class SampleStreamPodProxy(podKey: PodKey) : SampleStream, StreamingPodProxy<SampleArray, SampleStream>(
        pointedTo = podKey,
        converter = { it.nullableSampleArrayList() }
)

class SampleStreamMergingPodProxy(
        override val readsFrom: List<PodKey>
) : MergingPodProxy<SampleArray, SampleStream>(
        converter = { it.nullableSampleArrayList() }
), SampleStream