package io.wavebeans.lib.stream.window

import io.wavebeans.lib.Sample
import io.wavebeans.lib.ZeroSample
import io.wavebeans.lib.stream.SampleStream
import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor
import java.util.concurrent.TimeUnit

operator fun SampleWindowStream.minus(d: SampleWindowStream): SampleWindowStream =
        SampleMergedWindowStream(
                this,
                d,
                SampleMergedWindowStreamParams(
                        SampleDiffOperation,
                        this.parameters.windowSize,
                        this.parameters.step
                )
        )

operator fun SampleWindowStream.plus(d: SampleWindowStream): SampleWindowStream =
        SampleMergedWindowStream(
                this,
                d,
                SampleMergedWindowStreamParams(
                        SampleSumOperation,
                        this.parameters.windowSize,
                        this.parameters.step
                )
        )

@Serializer(forClass = SampleMergedWindowStreamOperation::class)
object SampleMergedWindowStreamOperationSerializer {

    override val descriptor: SerialDescriptor
        get() = StringDescriptor.withName("mergeOperation")

    override fun deserialize(decoder: Decoder): SampleMergedWindowStreamOperation =
            when (val operation = decoder.decodeString()) {
                "+" -> SampleSumOperation
                "-" -> SampleDiffOperation
                else -> throw UnsupportedOperationException("`$operation` is not supported")
            }

    override fun serialize(encoder: Encoder, obj: SampleMergedWindowStreamOperation) =
            when (obj) {
                is SampleDiffOperation -> encoder.encodeString("-")
                is SampleSumOperation -> encoder.encodeString("+")
                else -> throw UnsupportedOperationException("${obj::class} is not supported")
            }

}

@Serializer(forClass = SampleMergedWindowStreamParams::class)
object SampleMergedWindowStreamParamsSerializer {

    override val descriptor: SerialDescriptor
        get() = StringDescriptor.withName("windowMergeParams")

    override fun deserialize(decoder: Decoder): SampleMergedWindowStreamParams = TODO()

    override fun serialize(encoder: Encoder, obj: SampleMergedWindowStreamParams) = TODO()

}

@Serializable(with = SampleMergedWindowStreamParamsSerializer::class)
class SampleMergedWindowStreamParams(
        @Serializable(with = SampleMergedWindowStreamOperationSerializer::class) val operation: SampleMergedWindowStreamOperation,
        windowSize: Int,
        step: Int,
        start: Long = 0,
        end: Long? = null,
        timeUnit: TimeUnit = TimeUnit.MILLISECONDS
) : WindowStreamParams(windowSize, step, start, end, timeUnit)

object SampleSumOperation : SampleMergedWindowStreamOperation {
    override fun apply(a: Sample, b: Sample): Sample = a + b

}

object SampleDiffOperation : SampleMergedWindowStreamOperation {
    override fun apply(a: Sample, b: Sample): Sample = a - b
}

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

interface SampleMergedWindowStreamOperation : MergedWindowStreamOperation<Sample>