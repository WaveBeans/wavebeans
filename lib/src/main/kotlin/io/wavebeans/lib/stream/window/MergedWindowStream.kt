package io.wavebeans.lib.stream.window

import io.wavebeans.lib.Bean
import io.wavebeans.lib.BeanParams
import io.wavebeans.lib.MultiBean
import io.wavebeans.lib.Sample
import io.wavebeans.lib.stream.MergedStreamOperationSerializer
import io.wavebeans.lib.stream.SampleStream
import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor
import java.util.concurrent.TimeUnit


operator fun SampleWindowStream.minus(d: SampleWindowStream): SampleWindowStream =
        SampleMergedWindowStream(this, d, SampleMergedWindowStreamParams(0, SampleDiffOperation))
operator fun SampleWindowStream.plus(d: SampleWindowStream): SampleWindowStream =
        SampleMergedWindowStream(this, d, SampleMergedWindowStreamParams(0, SampleSumOperation))


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

@Serializable
data class SampleMergedWindowStreamParams(
        val shift: Int,
        @Serializable(with = MergedStreamOperationSerializer::class) val operation: SampleMergedWindowStreamOperation,
        val start: Long = 0,
        val end: Long? = null,
        val timeUnit: TimeUnit = TimeUnit.MILLISECONDS
) : BeanParams()

object SampleSumOperation : SampleMergedWindowStreamOperation {
    override fun elementApply(a: Sample, b: Sample): Sample = a + b

}

object SampleDiffOperation : SampleMergedWindowStreamOperation {
    override fun elementApply(a: Sample, b: Sample): Sample = a - b
}


class SampleMergedWindowStream(
        sourceStream: SampleWindowStream,
        mergeStream: SampleWindowStream,
        params: SampleMergedWindowStreamParams
) : MergedWindowStream<Sample, SampleStream, SampleWindowStream>(sourceStream, mergeStream, params.operation), SampleWindowStream

interface SampleMergedWindowStreamOperation: MergedWindowStreamOperation<Sample>


interface MergedWindowStreamOperation<T : Any> {

    fun apply(a: Window<T>, b: Window<T>): Window<T> = Window(a.elements.zip(b.elements).map { (a, b) -> elementApply(a, b) })

    fun elementApply(a: T, b: T): T
}



abstract class MergedWindowStream<T : Any, WINDOWING_STREAM : Any, S : WindowStream<T, WINDOWING_STREAM, S>>(
        sourceStream: WindowStream<T, WINDOWING_STREAM, S>,
        mergeStream: WindowStream<T, WINDOWING_STREAM, S>,
        fn: MergedWindowStreamOperation<T>
) : WindowStream<T, WINDOWING_STREAM, S>, MultiBean<Window<T>, S> {

    override val inputs: List<Bean<Window<T>, S>>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override val parameters: WindowStreamParams
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override fun asSequence(sampleRate: Float): Sequence<Window<T>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): S {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}

