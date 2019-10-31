package mux.lib.execution

import mux.lib.*
import mux.lib.stream.FiniteSampleStream
import java.util.concurrent.TimeUnit

class FiniteSampleStreamPod(
        val bean: FiniteSampleStream,
        podKey: PodKey,
        partition: Int
) : FiniteSampleStream, StreamingPod<SampleArray, FiniteSampleStream>(podKey, partition) {

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
        podKey: PodKey,
        partition: Int
) : StreamingPodProxy<SampleArray, FiniteSampleStream>(
        pointedTo = podKey,
        partition = partition,
        converter = { it.nullableSampleArrayList() }
), FiniteSampleStream {

    override fun length(timeUnit: TimeUnit): Long {
        val bush = podDiscovery.bushFor(pointedTo)
        val caller = bushCallerRepository.create(bush, pointedTo)

        return caller.call("length?timeUnit=${timeUnit.name}").long()
    }
}
class FiniteSampleStreamMergingPodProxy(
) : MergingPodProxy<SampleArray, FiniteSampleStream>(), FiniteSampleStream {

    override fun length(timeUnit: TimeUnit): Long {
        TODO()
    }
}