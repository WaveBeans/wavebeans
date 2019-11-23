package mux.lib.stream

import mux.lib.*
import mux.lib.io.StreamInput
import mux.lib.io.StreamOutput
import mux.lib.io.Writer
import java.util.concurrent.TimeUnit

fun FiniteSampleStream.toDevNull(
): StreamOutput<SampleArray, FiniteSampleStream> {
    return DevNullSampleStreamOutput(this, NoParams())
}

class DevNullSampleStreamOutput(
        val stream: FiniteSampleStream,
        params: NoParams
) : StreamOutput<SampleArray, FiniteSampleStream>, SinglePartitionBean {

    override fun writer(sampleRate: Float): Writer {

        val sampleIterator = stream.asSequence(sampleRate).iterator()
        var sampleCounter = 0L
        return object : Writer {
            override fun write(): Boolean {
                return if (sampleIterator.hasNext()) {
                    sampleCounter += sampleIterator.next().size
                    println("[/DEV/NULL] Written $sampleCounter samples")
                    true
                } else {
                    false
                }
            }

            override fun close() {}

        }
    }

    override val input: Bean<SampleArray, FiniteSampleStream>
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

    override fun asSequence(sampleRate: Float): Sequence<SampleArray> {
        return seq.windowed(DEFAULT_SAMPLE_ARRAY_SIZE, DEFAULT_SAMPLE_ARRAY_SIZE, true)
                .map { createSampleArray(it.size) { i -> it[i] } }
    }


}