package io.wavebeans.lib.stream.window

import io.wavebeans.lib.*
import io.wavebeans.lib.stream.SampleStream
import io.wavebeans.lib.stream.StreamOp
import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor
import java.util.concurrent.TimeUnit

operator fun SampleWindowStream.minus(d: SampleWindowStream): SampleWindowStream =
        SampleMergedWindowStream(
                this,
                d,
                SampleMergedWindowStreamParams(SampleMinusStreamOp, this.parameters.windowSize, this.parameters.step)
        )

operator fun SampleWindowStream.plus(d: SampleWindowStream): SampleWindowStream =
        SampleMergedWindowStream(
                this,
                d,
                SampleMergedWindowStreamParams(SamplePlusStreamOp, this.parameters.windowSize, this.parameters.step)
        )

operator fun SampleWindowStream.times(d: SampleWindowStream): SampleWindowStream =
        SampleMergedWindowStream(
                this,
                d,
                SampleMergedWindowStreamParams(SampleTimesStreamOp, this.parameters.windowSize, this.parameters.step)
        )

operator fun SampleWindowStream.div(d: SampleWindowStream): SampleWindowStream =
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
        step: Int,
        start: Long = 0,
        end: Long? = null,
        timeUnit: TimeUnit = TimeUnit.MILLISECONDS
) : WindowStreamParams(windowSize, step, start, end, timeUnit)

class SampleMergedWindowStream(
        sourceStream: SampleWindowStream,
        mergeStream: SampleWindowStream,
        val params: SampleMergedWindowStreamParams
) : MergedWindowStream<Sample, SampleStream, SampleWindowStream>(sourceStream, mergeStream, params.operation), SampleWindowStream {
    override val zeroEl: Sample
        get() = ZeroSample

    override val parameters: WindowStreamParams
        get() = params

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): SampleWindowStream {
        return SampleMergedWindowStream(
                sourceStream.rangeProjection(start, end, timeUnit),
                mergeStream.rangeProjection(start, end, timeUnit),
                params
        )
    }
}