package mux.lib.execution

import mux.lib.Bean
import mux.lib.BeanParams
import mux.lib.NoParams
import mux.lib.Sample
import mux.lib.stream.SampleStream
import java.util.concurrent.TimeUnit

class SampleStreamPod(
        val bean: SampleStream,
        podKey: PodKey
) : StreamingPod<Sample, SampleStream>(podKey), SampleStream, Pod<Sample, SampleStream> {

    override val parameters: BeanParams = NoParams()

    override fun asSequence(sampleRate: Float): Sequence<Sample> = bean.asSequence(sampleRate)

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): SampleStream {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun inputs(): List<Bean<*, *>> = listOf(bean)
}

class SampleStreamPodProxy(podKey: PodKey) : SampleStream, AbstractStreamPodProxy<SampleStream>(podKey)