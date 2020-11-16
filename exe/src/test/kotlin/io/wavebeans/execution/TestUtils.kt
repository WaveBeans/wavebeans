package io.wavebeans.execution

import assertk.Assert
import assertk.all
import assertk.assertions.isEqualTo
import assertk.assertions.support.fail
import assertk.assertions.support.show
import io.wavebeans.execution.pod.PodKey
import io.wavebeans.execution.pod.SplittingPod
import io.wavebeans.execution.pod.StreamingPod
import io.wavebeans.lib.*

class IntStream(
        val seq: List<Int>
) : BeanStream<Sample> {
    override fun asSequence(sampleRate: Float): Sequence<Sample> = seq.asSequence().map { sampleOf(it) }

    override fun inputs(): List<AnyBean> = throw UnsupportedOperationException()

    override val parameters: BeanParams
        get() = throw UnsupportedOperationException()

    override val desiredSampleRate: Float? = null

}

fun newTestStreamingPod(seq: List<Int>, partition: Int = 0, partitionSize: Int = 1): StreamingPod<Sample, IntStream> {
    return object : StreamingPod<Sample, IntStream>(
            bean = IntStream(seq),
            podKey = PodKey(1, partition),
            partitionSize = partitionSize
    ) {}
}

fun newTestSplittingPod(seq: List<Int>, partitionCount: Int): SplittingPod<Sample, IntStream> {
    return object : SplittingPod<Sample, IntStream>(
            bean = IntStream(seq),
            podKey = PodKey(1, 0),
            partitionCount = partitionCount,
            partitionSize = 1
    ) {}
}

infix fun Int.to(to: Int) = BeanLink(this.toLong(), to.toLong())

infix fun Double.to(to: Double) = BeanLink(
        from = this.toLong(),
        fromPartition = (this * 10.0).toInt() % 10,
        to = to.toLong(),
        toPartition = (to * 10.0).toInt() % 10
)

infix fun BeanLink.order(order: Int) = this.copy(order = order)

fun <T> Assert<List<T>>.isListOf(vararg expected: Any?) = given { actual ->
    if (actual == expected.toList()) return
    fail(expected, actual)
}

fun seqStream() = SeqInput()

class SeqInput constructor(
        val params: NoParams = NoParams()
) : BeanStream<Sample>, SourceBean<Sample>, SinglePartitionBean {

    private val seq = (0..10_000_000_000).asSequence().map { 1e-10 * it }

    override val parameters: BeanParams = params

    override fun asSequence(sampleRate: Float): Sequence<Sample> = seq

    override val desiredSampleRate: Float? = null
}

fun <T> Assert<Iterable<T>>.eachIndexed(expectedSize: Int? = null, f: (Assert<T>, Int) -> Unit) = given { actual ->
    all {
        expectedSize?.let { assertThat(actual.count(), "elements count").isEqualTo(it) }
        actual.forEachIndexed { index, item ->
            f(assertThat(item, name = "${name ?: ""}${show(index, "[]")}"), index)
        }
    }
}

