package mux.lib.stream

import mux.lib.*
import mux.lib.io.StreamInput
import java.util.concurrent.TimeUnit

fun StreamInput.sampleStream(): SampleStream = InfiniteSampleStream(this)

class InfiniteSampleStream(
        val streamInput: StreamInput
) : SampleStream, AlterMuxNode<Sample, StreamInput, Sample, SampleStream> {

    override val input: MuxNode<Sample, StreamInput> = streamInput

    override fun asSequence(sampleRate: Float): Sequence<Sample> = streamInput.asSequence(sampleRate)

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): InfiniteSampleStream =
            InfiniteSampleStream(streamInput.rangeProjection(start, end, timeUnit))

}