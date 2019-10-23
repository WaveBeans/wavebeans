package mux.lib.execution

import mux.lib.*
import mux.lib.stream.FiniteSampleStream
import java.util.concurrent.TimeUnit

class FiniteSampleStreamPod(
        val bean: FiniteSampleStream,
        podKey: PodKey
) : FiniteSampleStream, StreamingPod<SampleArray, FiniteSampleStream>(podKey) {

    override fun length(timeUnit: TimeUnit): Long {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun asSequence(sampleRate: Float): Sequence<SampleArray> = bean.asSequence(sampleRate)

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): FiniteSampleStream {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun inputs(): List<Bean<*, *>> = listOf(bean)

    override val parameters: BeanParams = NoParams()

}

@ExperimentalStdlibApi
class FiniteSampleStreamPodProxy(
        podKey: PodKey
) : StreamingPodProxy<SampleArray, FiniteSampleStream>(
        pointedTo = podKey,
        converter = { it.nullableSampleArrayList() }
), FiniteSampleStream {

    override fun length(timeUnit: TimeUnit): Long {
        val bush = podDiscovery.bushFor(pointedTo)
        val caller = bushCallerRepository.create(bush, pointedTo)

        return caller.call("length?timeUnit=${timeUnit.name}").long()
    }
}