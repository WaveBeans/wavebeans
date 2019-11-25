package io.wavebeans.lib.stream

import io.wavebeans.lib.*
import io.wavebeans.lib.io.FiniteInput
import java.util.concurrent.TimeUnit

fun FiniteInput.finiteSampleStream(): FiniteSampleStream = FiniteInputSampleStream(this, NoParams())

fun FiniteInput.sampleStream(converter: FiniteToStream): SampleStream = this.finiteSampleStream().sampleStream(converter)

class FiniteInputSampleStream(
        val finiteInput: FiniteInput,
        val params: NoParams
) : FiniteSampleStream, AlterBean<SampleArray, FiniteInput, SampleArray, FiniteSampleStream> {

    override val parameters: BeanParams = params

    override val input: Bean<SampleArray, FiniteInput> = finiteInput

    override fun asSequence(sampleRate: Float): Sequence<SampleArray> = finiteInput.asSequence(sampleRate)

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): FiniteInputSampleStream =
            FiniteInputSampleStream(finiteInput.rangeProjection(start, end, timeUnit), params)

    override fun length(timeUnit: TimeUnit): Long = finiteInput.length(timeUnit)

}

