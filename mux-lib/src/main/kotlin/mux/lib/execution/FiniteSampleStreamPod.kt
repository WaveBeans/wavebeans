package mux.lib.execution

import mux.lib.*
import mux.lib.stream.FiniteSampleStream
import java.util.concurrent.TimeUnit

class FiniteSampleStreamStreamingPod(
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

class FiniteSampleStreamSplittingPod(
        val bean: FiniteSampleStream,
        podKey: PodKey,
        partitionCount: Int
) : FiniteSampleStream, SplittingPod<SampleArray, FiniteSampleStream>(podKey, partitionCount) {

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