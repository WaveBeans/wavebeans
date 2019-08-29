package mux.lib.stream

import mux.lib.Sample
import mux.lib.io.FiniteInput
import mux.lib.io.StreamInput
import java.util.concurrent.TimeUnit


fun StreamInput.sampleStream(): SampleStream = InfiniteSampleStream(this)


class InfiniteSampleStream(
        val input: StreamInput
) : SampleStream {

    override fun asSequence(sampleRate: Float): Sequence<Sample> = input.asSequence(sampleRate)

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): InfiniteSampleStream =
            InfiniteSampleStream(input.rangeProjection(start, end, timeUnit))

}