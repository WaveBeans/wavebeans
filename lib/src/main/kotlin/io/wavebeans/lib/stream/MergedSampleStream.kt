package io.wavebeans.lib.stream

import io.wavebeans.lib.*
import kotlinx.serialization.*

operator fun BeanStream<Sample>.minus(d: BeanStream<Sample>): BeanStream<Sample> = diff(this, d)

operator fun BeanStream<Sample>.plus(d: BeanStream<Sample>): BeanStream<Sample> = sum(this, d)

fun diff(x: BeanStream<Sample>, y: BeanStream<Sample>, shift: Int = 0) = MergedSampleStream(
        x,
        y,
        MergedSampleStreamParams(shift, SampleMinusStreamOp)
)

fun sum(x: BeanStream<Sample>, y: BeanStream<Sample>, shift: Int = 0) = MergedSampleStream(
        x,
        y,
        MergedSampleStreamParams(shift, SamplePlusStreamOp)
)

@Serializable
data class MergedSampleStreamParams(
        val shift: Int,
        @Serializable(with = StreamOpSerializer::class) val operation: StreamOp<Sample>
) : BeanParams()

// TODO that can merge any type of stream in generic way
class MergedSampleStream(
        val sourceStream: BeanStream<Sample>,
        val mergingStream: BeanStream<Sample>,
        val params: MergedSampleStreamParams
) : BeanStream<Sample>, MultiBean<Sample>, SinglePartitionBean {

    override val parameters: BeanParams = params

    override val inputs: List<Bean<Sample>> = listOf(sourceStream, mergingStream)

    override fun asSequence(sampleRate: Float): Sequence<Sample> {

        var newShift: Long
        if (params.shift < 0) {
            newShift = params.shift.toLong() + 0L
            if (newShift > 0) newShift = 0L
        } else {
            newShift = params.shift - 0L
            if (newShift < 0) newShift = 0L
        }

        val sourceStreamIterator = sourceStream.asSequence(sampleRate).iterator()
        val mergingStreamIterator = mergingStream.asSequence(sampleRate).iterator()

        var sourceStreamPos = 0L
        var mergingStreamPos = if (newShift < 0) newShift else 0L

        return object : Iterator<Sample> {

            override fun hasNext(): Boolean = sourceStreamIterator.hasNext() || mergingStreamIterator.hasNext()

            override fun next(): Sample {
                return when {
                    mergingStreamPos < 0 -> {
                        mergingStreamPos++

                        val sourceSample = ZeroSample
                        val mergingSample =
                                if (mergingStreamIterator.hasNext()) mergingStreamIterator.next()
                                else ZeroSample

                        params.operation.apply(sourceSample, mergingSample)
                    }
                    mergingStreamPos < newShift -> {
                        mergingStreamPos++
                        sourceStreamPos++

                        val sourceSample =
                                if (sourceStreamIterator.hasNext()) sourceStreamIterator.next()
                                else ZeroSample
                        val mergingSample = ZeroSample

                        params.operation.apply(sourceSample, mergingSample)
                    }
                    else -> {
                        mergingStreamPos++
                        sourceStreamPos++

                        val sourceSample =
                                if (sourceStreamIterator.hasNext()) sourceStreamIterator.next()
                                else ZeroSample
                        val mergingSample =
                                if (mergingStreamIterator.hasNext()) mergingStreamIterator.next()
                                else ZeroSample

                        params.operation.apply(sourceSample, mergingSample)
                    }
                }
            }
        }.asSequence()

    }
}
