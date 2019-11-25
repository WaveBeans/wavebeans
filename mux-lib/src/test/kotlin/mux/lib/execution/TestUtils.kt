package mux.lib.execution

import mux.lib.*
import java.util.concurrent.TimeUnit

class IntStream(
        val seq: List<Int>
) : BeanStream<Sample, IntStream> {
    override fun asSequence(sampleRate: Float): Sequence<Sample> = seq.asSequence().map { sampleOf(it) }

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): IntStream = throw UnsupportedOperationException()

    override fun inputs(): List<Bean<*, *>> = throw UnsupportedOperationException()

    override val parameters: BeanParams
        get() = throw UnsupportedOperationException()

}

fun newTestStreamingPod(seq: List<Int>, partition: Int = 0): StreamingPod {
    return StreamingPod(
            IntStream(seq),
            PodKey(1, partition)
    )
}

fun newTestSplittingPod(seq: List<Int>, partitionCount: Int): SplittingPod {
    return SplittingPod(
            IntStream(seq),
            PodKey(1, 0),
            partitionCount
    )
}

infix fun Int.to(to: Int) = BeanLink(this, to)

infix fun Double.to(to: Double) = BeanLink(
        from = this.toInt(),
        fromPartition = (this * 10.0).toInt() % 10,
        to = to.toInt(),
        toPartition = (to * 10.0).toInt() % 10
)

infix fun BeanLink.order(order: Int) = this.copy(order = order)

