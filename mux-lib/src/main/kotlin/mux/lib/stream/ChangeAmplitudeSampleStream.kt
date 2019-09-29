package mux.lib.stream

import kotlinx.serialization.Serializable
import mux.lib.MuxNode
import mux.lib.MuxParams
import mux.lib.SingleMuxNode
import mux.lib.Sample
import java.util.concurrent.TimeUnit

fun SampleStream.changeAmplitude(multiplier: Double): SampleStream =
        ChangeAmplitudeSampleStream(this, ChangeAmplitudeSampleStreamParams(multiplier))

@Serializable
data class ChangeAmplitudeSampleStreamParams(
        val multiplier: Double
) : MuxParams()

class ChangeAmplitudeSampleStream(
        val source: SampleStream,
        val params: ChangeAmplitudeSampleStreamParams
) : SampleStream, SingleMuxNode<Sample, SampleStream> {

    override val parameters: MuxParams = params

    override val input: MuxNode<Sample, SampleStream> = source

    override fun asSequence(sampleRate: Float): Sequence<Sample> {
        return source.asSequence(sampleRate).map { it * params.multiplier }
    }

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): SampleStream {
        return ChangeAmplitudeSampleStream(source.rangeProjection(start, end, timeUnit), params)
    }
}