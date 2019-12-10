package io.wavebeans.lib.stream.window

import io.wavebeans.lib.*
import io.wavebeans.lib.stream.StreamOp
import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor

operator fun WindowStream<Sample>.minus(d: WindowStream<Sample>): WindowStream<Sample> =
        SampleMergedWindowStream(
                this,
                d,
                SampleMergedWindowStreamParams(SampleMinusStreamOp, this.parameters.windowSize, this.parameters.step)
        )

operator fun WindowStream<Sample>.plus(d: WindowStream<Sample>): WindowStream<Sample> =
        SampleMergedWindowStream(
                this,
                d,
                SampleMergedWindowStreamParams(SamplePlusStreamOp, this.parameters.windowSize, this.parameters.step)
        )

operator fun WindowStream<Sample>.times(d: WindowStream<Sample>): WindowStream<Sample> =
        SampleMergedWindowStream(
                this,
                d,
                SampleMergedWindowStreamParams(SampleTimesStreamOp, this.parameters.windowSize, this.parameters.step)
        )

operator fun WindowStream<Sample>.div(d: WindowStream<Sample>): WindowStream<Sample> =
        SampleMergedWindowStream(
                this,
                d,
                SampleMergedWindowStreamParams(SampleDivStreamOp, this.parameters.windowSize, this.parameters.step)
        )


@Serializer(forClass = SampleMergedWindowStreamParams::class)
object SampleMergedWindowStreamParamsSerializer {

    override val descriptor: SerialDescriptor
        get() = StringDescriptor.withName("windowMergeParams")

    override fun deserialize(decoder: Decoder): SampleMergedWindowStreamParams = TODO()

    override fun serialize(encoder: Encoder, obj: SampleMergedWindowStreamParams) = TODO()

}

@Serializable(with = SampleMergedWindowStreamParamsSerializer::class)
class SampleMergedWindowStreamParams(
        @Serializable(with = StreamOpSerializer::class) val operation: StreamOp<Sample>,
        windowSize: Int,
        step: Int
) : WindowStreamParams(windowSize, step)

class SampleMergedWindowStream(
        sourceStream: WindowStream<Sample>,
        mergeStream: WindowStream<Sample>,
        val params: SampleMergedWindowStreamParams
) : MergedWindowStream<Sample>(sourceStream, mergeStream, params.operation), WindowStream<Sample> {
    override val zeroEl: Sample
        get() = ZeroSample

    override val parameters: WindowStreamParams
        get() = params
}