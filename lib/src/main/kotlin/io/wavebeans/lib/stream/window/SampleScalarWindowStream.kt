package io.wavebeans.lib.stream.window

import io.wavebeans.lib.*
import io.wavebeans.lib.stream.ScalarOp
import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor

operator fun WindowStream<Sample>.minus(d: Number): WindowStream<Sample> =
        SampleScalarWindowStream(
                this,
                SampleScalarWindowStreamParams(SampleMinusScalarOp, d.toDouble(), this.parameters.windowSize, this.parameters.step)
        )

operator fun WindowStream<Sample>.plus(d: Number): WindowStream<Sample> =
        SampleScalarWindowStream(
                this,
                SampleScalarWindowStreamParams(SamplePlusScalarOp, d.toDouble(), this.parameters.windowSize, this.parameters.step)
        )

operator fun WindowStream<Sample>.times(d: Number): WindowStream<Sample> =
        SampleScalarWindowStream(
                this,
                SampleScalarWindowStreamParams(SampleTimesScalarOp, d.toDouble(), this.parameters.windowSize, this.parameters.step)
        )

operator fun WindowStream<Sample>.div(d: Number): WindowStream<Sample> =
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

@Serializable(with = SampleScalarWindowStreamParamsSerializer::class)
class SampleScalarWindowStreamParams(
        @Serializable(with = ScalarOpSerializer::class) val operation: ScalarOp<Double, Sample>,
        val scalar: Double,
        windowSize: Int,
        step: Int
) : WindowStreamParams(windowSize, step)

class SampleScalarWindowStream(
        sourceStream: WindowStream<Sample>,
        val params: SampleScalarWindowStreamParams
) : ScalarOpWindowStream<Double, Sample>(
        sourceStream,
        params.operation,
        params.scalar
), WindowStream<Sample> {

    override val input: Bean<Window<Sample>>
        get() = sourceStream

    override val parameters: WindowStreamParams
        get() = params
}