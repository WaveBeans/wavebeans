package mux.lib.stream

import mux.lib.*
import mux.lib.io.StreamInput
import java.util.concurrent.TimeUnit

fun StreamInput.sampleStream(): SampleStream = InfiniteSampleStream(this, NoParams())

class InfiniteSampleStream(
        val streamInput: StreamInput,
        val params: NoParams
) : SampleStream, AlterBean<SampleArray, StreamInput, SampleArray, SampleStream> {

    override val parameters: BeanParams = params

    override val input: Bean<SampleArray, StreamInput> = streamInput

    override fun asSequence(sampleRate: Float): Sequence<SampleArray> = streamInput.asSequence(sampleRate)

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): InfiniteSampleStream =
            InfiniteSampleStream(streamInput.rangeProjection(start, end, timeUnit), params)

}