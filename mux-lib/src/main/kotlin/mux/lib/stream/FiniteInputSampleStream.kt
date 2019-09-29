package mux.lib.stream

import mux.lib.*
import mux.lib.io.FiniteInput
import java.util.concurrent.TimeUnit

fun FiniteInput.finiteSampleStream(): FiniteSampleStream = FiniteInputSampleStream(this, NoParams())

fun FiniteInput.sampleStream(converter: FiniteToStream): SampleStream = this.finiteSampleStream().sampleStream(converter)

class FiniteInputSampleStream(
        val finiteInput: FiniteInput,
        val params: NoParams
) : FiniteSampleStream, AlterMuxNode<Sample, FiniteInput, Sample, FiniteSampleStream> {

    override val parameters: MuxParams = params

    override val input: MuxNode<Sample, FiniteInput> = finiteInput

    override fun asSequence(sampleRate: Float): Sequence<Sample> = finiteInput.asSequence(sampleRate)

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): FiniteInputSampleStream =
            FiniteInputSampleStream(finiteInput.rangeProjection(start, end, timeUnit), params)

    override fun length(timeUnit: TimeUnit): Long = finiteInput.length(timeUnit)

}

