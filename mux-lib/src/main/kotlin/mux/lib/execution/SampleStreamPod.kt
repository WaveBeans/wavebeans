package mux.lib.execution

import mux.lib.*
import mux.lib.stream.SampleStream
import java.util.concurrent.TimeUnit

class SampleStreamPod(
        val bean: SampleStream,
        podKey: PodKey,
        partition: Int
) : StreamingPod<SampleArray, SampleStream>(podKey, partition), SampleStream, Pod<SampleArray, SampleStream> {

    override val parameters: BeanParams = NoParams()

    override fun asSequence(sampleRate: Float): Sequence<SampleArray> = bean.asSequence(sampleRate)

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): SampleStream {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun inputs(): List<Bean<*, *>> = listOf(bean)
}

class SampleStreamPodProxy(podKey: PodKey, partition: Int) : SampleStream, StreamingPodProxy<SampleArray, SampleStream>(
        pointedTo = podKey,
        partition = partition,
        converter = { it.nullableSampleArrayList() }
)

class SampleStreamMergingPodProxy() : SampleStream, MergingPodProxy<SampleArray, SampleStream>(
)