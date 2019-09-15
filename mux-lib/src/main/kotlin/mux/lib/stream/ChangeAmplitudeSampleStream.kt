package mux.lib.stream

import mux.lib.MuxNode
import mux.lib.SingleMuxNode
import mux.lib.Sample
import java.util.concurrent.TimeUnit

fun SampleStream.changeAmplitude(multiplier: Double): SampleStream =
        ChangeAmplitudeSampleStream(this, multiplier)

class ChangeAmplitudeSampleStream(
        val sourceNode: SampleStream,
        val multiplier: Double
) : SampleStream, SingleMuxNode<Sample, SampleStream> {

    override val input: MuxNode<Sample, SampleStream> = sourceNode

    override fun asSequence(sampleRate: Float): Sequence<Sample> {
        return sourceNode.asSequence(sampleRate).map { it * multiplier }
    }

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): SampleStream {
        return ChangeAmplitudeSampleStream(sourceNode.rangeProjection(start, end, timeUnit), multiplier)
    }
}