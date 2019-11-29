package io.wavebeans.execution

import assertk.Assert
import assertk.all
import assertk.assertions.isEqualTo
import assertk.assertions.support.fail
import assertk.assertions.support.show
import io.wavebeans.lib.*
import io.wavebeans.lib.io.StreamInput
import io.wavebeans.lib.io.StreamOutput
import io.wavebeans.lib.io.Writer
import io.wavebeans.lib.stream.FiniteSampleStream
import io.wavebeans.lib.stream.InfiniteSampleStream
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

fun newTestStreamingPod(seq: List<Int>, partition: Int = 0, partitionSize: Int = 1): StreamingPod<Sample, SampleArray> {
    return object : StreamingPod<Sample, SampleArray>(
            bean = IntStream(seq),
            podKey = PodKey(1, partition),
            partitionSize = partitionSize,
            converter = { list ->
                val i = list.iterator()
                createSampleArray(list.size) { i.next() }
            }
    ) {}
}

fun newTestSplittingPod(seq: List<Int>, partitionCount: Int): SplittingPod<Sample, SampleArray> {
    return object : SplittingPod<Sample, SampleArray>(
            bean = IntStream(seq),
            podKey = PodKey(1, 0),
            partitionCount = partitionCount,
            partitionSize = 1,
            converter = { list ->
                val i = list.iterator()
                createSampleArray(list.size) { i.next() }
            }
    ) {}
}

infix fun Int.to(to: Int) = BeanLink(this, to)

infix fun Double.to(to: Double) = BeanLink(
        from = this.toInt(),
        fromPartition = (this * 10.0).toInt() % 10,
        to = to.toInt(),
        toPartition = (to * 10.0).toInt() % 10
)

infix fun BeanLink.order(order: Int) = this.copy(order = order)

fun <T> Assert<List<T>>.isListOf(vararg expected: Any?) = given { actual ->
    if (actual == expected.toList()) return
    fail(expected, actual)
}

fun FiniteSampleStream.toDevNull(
): StreamOutput<Sample, FiniteSampleStream> {
    return DevNullSampleStreamOutput(this, NoParams())
}

class DevNullSampleStreamOutput(
        val stream: FiniteSampleStream,
        params: NoParams
) : StreamOutput<Sample, FiniteSampleStream>, SinglePartitionBean {

    override fun writer(sampleRate: Float): Writer {

        val sampleIterator = stream.asSequence(sampleRate).iterator()
        var sampleCounter = 0L
        return object : Writer {
            override fun write(): Boolean {
                return if (sampleIterator.hasNext()) {
                    sampleIterator.next()
                    sampleCounter++
                    true
                } else {
                    false
                }
            }

            override fun close() {
                println("[/DEV/NULL] Written $sampleCounter samples")
            }

        }
    }

    override val input: Bean<Sample, FiniteSampleStream>
        get() = stream

    override val parameters: BeanParams = params

}


fun seqStream() = InfiniteSampleStream(SeqInput(NoParams()), NoParams())

class SeqInput constructor(
        val params: NoParams
) : StreamInput, SinglePartitionBean {

    private val seq = (0..10_000_000_000).asSequence().map { 1e-10 * it }

    override val parameters: BeanParams = params

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): StreamInput = throw UnsupportedOperationException()

    override fun asSequence(sampleRate: Float): Sequence<Sample> = seq


}

fun <T> Assert<Iterable<T>>.eachIndexed(expectedSize: Int? = null, f: (Assert<T>, Int) -> Unit) = given { actual ->
    all {
        expectedSize?.let { assertThat(actual.count(), "elements count").isEqualTo(it) }
        actual.forEachIndexed { index, item ->
            f(assertThat(item, name = "${name ?: ""}${show(index, "[]")}"), index)
        }
    }
}

