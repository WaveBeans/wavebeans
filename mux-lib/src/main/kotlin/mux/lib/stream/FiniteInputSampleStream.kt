package mux.lib.stream

import mux.lib.Mux
import mux.lib.MuxNode
import mux.lib.MuxSingleInputNode
import mux.lib.Sample
import mux.lib.io.FiniteInput
import java.util.concurrent.TimeUnit

fun FiniteInput.finiteSampleStream(): FiniteSampleStream = FiniteInputSampleStream(this)

fun FiniteInput.sampleStream(converter: FiniteToStream): SampleStream = this.finiteSampleStream().sampleStream(converter)

class FiniteInputSampleStream(
        val input: FiniteInput
) : FiniteSampleStream {

    override fun mux(): MuxNode = MuxSingleInputNode(Mux("FiniteInputSampleStream"), input.mux())

    override fun asSequence(sampleRate: Float): Sequence<Sample> = input.asSequence(sampleRate)

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): FiniteInputSampleStream =
            FiniteInputSampleStream(input.rangeProjection(start, end, timeUnit))

    override fun length(timeUnit: TimeUnit): Long = input.length(timeUnit)

}

