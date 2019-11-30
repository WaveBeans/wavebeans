package io.wavebeans.lib.stream.window

import io.wavebeans.lib.*
import io.wavebeans.lib.stream.SampleStream
import io.wavebeans.lib.stream.ScalarOp
import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor
import java.util.concurrent.TimeUnit

operator fun SampleWindowStream.minus(d: Number): SampleWindowStream =
        SampleScalarWindowStream(
                this,
                SampleScalarWindowStreamParams(SampleMinusScalarOp, d.toDouble(), this.parameters.windowSize, this.parameters.step)
        )

operator fun SampleWindowStream.plus(d: Number): SampleWindowStream =
        SampleScalarWindowStream(
                this,
                SampleScalarWindowStreamParams(SamplePlusScalarOp, d.toDouble(), this.parameters.windowSize, this.parameters.step)
        )

operator fun SampleWindowStream.times(d: Number): SampleWindowStream =
        SampleScalarWindowStream(
                this,
                SampleScalarWindowStreamParams(SampleTimesScalarOp, d.toDouble(), this.parameters.windowSize, this.parameters.step)
        )

operator fun SampleWindowStream.div(d: Number): SampleWindowStream =
        SampleScalarWindowStream(
                this,
                SampleScalarWindowStreamParams(SampleDivScalarOp, d.toDouble(), this.parameters.windowSize, this.parameters.step)
        )


@Serializer(forClass = SampleScalarWindowStreamParams::class)
object SampleScalarWindowStreamParamsSerializer {

    override val descriptor: SerialDescriptor
        get() = StringDescriptor.withName("windowMergeParams")

    override fun deserialize(decoder: Decoder): SampleScalarWindowStreamParams = TODO()

    override fun serialize(encoder: Encoder, obj: SampleScalarWindowStreamParams) = TODO()

}

@Serializable(with = SampleMergedWindowStreamParamsSerializer::class)
class SampleScalarWindowStreamParams(
        @Serializable(with = ScalarOpSerializer::class) val operation: ScalarOp<Double, Sample>,
        val scalar: Double,
        windowSize: Int,
        step: Int,
        start: Long = 0,
        end: Long? = null,
        timeUnit: TimeUnit = TimeUnit.MILLISECONDS
) : WindowStreamParams(windowSize, step, start, end, timeUnit)

class SampleScalarWindowStream(
        sourceStream: SampleWindowStream,
        val params: SampleScalarWindowStreamParams
) : ScalarOpWindowStream<Double, Sample, SampleStream, SampleWindowStream>(
        sourceStream,
        params.operation,
        params.scalar
), SampleWindowStream {

    override val input: Bean<Window<Sample>, SampleWindowStream>
        get() = sourceStream

    override val parameters: WindowStreamParams
        get() = params

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): SampleWindowStream {
        return SampleScalarWindowStream(sourceStream.rangeProjection(start, end, timeUnit), params)
    }
}