package mux.lib.stream

import mux.lib.Sample
import mux.lib.io.FiniteInput
import java.util.concurrent.TimeUnit

fun FiniteInput.finiteSampleStream(): FiniteSampleStream = FiniteInputSampleStream(this)

fun FiniteInput.sampleStreamWithZeroFilling(): SampleStream = ZeroFillingFiniteSampleStream(this.finiteSampleStream())

class FiniteInputSampleStream(
        val input: FiniteInput
) : FiniteSampleStream {

    override fun asSequence(sampleRate: Float): Sequence<Sample> = input.asSequence(sampleRate)

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): FiniteInputSampleStream =
            FiniteInputSampleStream(input.rangeProjection(start, end, timeUnit))

    override fun length(timeUnit: TimeUnit): Long = input.length(timeUnit)

}

