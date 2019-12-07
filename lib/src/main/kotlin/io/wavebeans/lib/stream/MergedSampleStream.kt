package io.wavebeans.lib.stream

import io.wavebeans.lib.*
import kotlinx.serialization.*
import java.util.concurrent.TimeUnit
import kotlin.math.max

operator fun SampleStream.minus(d: SampleStream): SampleStream = diff(this, d)

operator fun SampleStream.plus(d: SampleStream): SampleStream = sum(this, d)

fun diff(x: SampleStream, y: SampleStream, shift: Int = 0) = MergedSampleStream(
        x,
        y,
        MergedSampleStreamParams(shift, SampleMinusStreamOp)
)

fun sum(x: SampleStream, y: SampleStream, shift: Int = 0) = MergedSampleStream(
        x,
        y,
        MergedSampleStreamParams(shift, SamplePlusStreamOp)
)

@Serializable
data class MergedSampleStreamParams(
        val shift: Int,
        @Serializable(with = StreamOpSerializer::class) val operation: StreamOp<Sample>,
        val start: Long = 0,
        val end: Long? = null,
        val timeUnit: TimeUnit = TimeUnit.MILLISECONDS
) : BeanParams()

// TODO that can merge any type of stream in generic way
class MergedSampleStream(
        val sourceStream: SampleStream,
        val mergingStream: SampleStream,
        val params: MergedSampleStreamParams
) : SampleStream, MultiBean<Sample, SampleStream>, SinglePartitionBean {

    override val parameters: BeanParams = params

    override val inputs: List<Bean<Sample, SampleStream>> = listOf(sourceStream, mergingStream)

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): SampleStream {
        return MergedSampleStream(
                sourceStream,
                mergingStream,
                params.copy(
                        start = start + params.start,
                        end = end?.let { it + (params.end ?: 0) },
                        timeUnit = timeUnit
                )
        )
    }

    override fun asSequence(sampleRate: Float): Sequence<Sample> {
        val s = max(timeToSampleIndexFloor(params.start, params.timeUnit, sampleRate), 0L)
        val e = params.end?.let { timeToSampleIndexCeil(it, params.timeUnit, sampleRate) }
        val l = e?.minus(s)

        var ss: Long
        var ms: Long
        var newShift: Long
        if (params.shift < 0) {
            ss = s + params.shift
            if (ss < 0) ss = 0
            ms = s
            newShift = params.shift.toLong() + s
            if (newShift > 0) newShift = 0L
        } else {
            ss = s
            ms = s - params.shift
            if (ms < 0) ms = 0
            newShift = params.shift - s
            if (newShift < 0) newShift = 0L
        }

        val sourceStreamIterator =
                (if (params.start != 0L || params.end != null)
                    sourceStream.rangeProjection(
                            samplesCountToLength(ss, sampleRate, params.timeUnit),
                            l?.plus(ss)?.let { samplesCountToLength(it, sampleRate, params.timeUnit) },
                            params.timeUnit
                    )
                else sourceStream)
                        .asSequence(sampleRate).iterator()
        val mergingStreamIterator =
                (if (params.start != 0L || params.end != null)
                    mergingStream.rangeProjection(
                            samplesCountToLength(ms, sampleRate, params.timeUnit),
                            l?.plus(ms)?.let { samplesCountToLength(it, sampleRate, params.timeUnit) },
                            params.timeUnit
                    )
                else mergingStream)
                        .asSequence(sampleRate).iterator()

        var globalCounter = 0
        var sourceStreamPos = 0L
        var mergingStreamPos = if (newShift < 0) newShift else 0L

        return object : Iterator<Sample> {

            override fun hasNext(): Boolean {
                return !(l != null && globalCounter >= l)
            }

            override fun next(): Sample {
                globalCounter++
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
