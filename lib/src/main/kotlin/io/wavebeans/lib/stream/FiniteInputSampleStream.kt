package io.wavebeans.lib.stream

import io.wavebeans.lib.*
import io.wavebeans.lib.io.FiniteInput
import java.util.concurrent.TimeUnit

fun FiniteInput.finiteSampleStream(): FiniteSampleStream = FiniteInputSampleStream(this, NoParams())

fun FiniteInput.sampleStream(converter: FiniteToStream): BeanStream<Sample> = this.finiteSampleStream().sampleStream(converter)

class FiniteInputSampleStream(
        val finiteInput: FiniteInput,
        val params: NoParams
) : FiniteSampleStream, SingleBean<Sample> {

    override val parameters: BeanParams = params

    override val input: Bean<Sample> = finiteInput

    override fun asSequence(sampleRate: Float): Sequence<Sample> = finiteInput.asSequence(sampleRate)

    override fun length(timeUnit: TimeUnit): Long = finiteInput.length(timeUnit)

}

