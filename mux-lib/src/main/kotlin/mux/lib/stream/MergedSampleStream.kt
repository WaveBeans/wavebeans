package mux.lib.stream

import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor
import mux.lib.*
import java.util.concurrent.TimeUnit
import kotlin.math.max

operator fun SampleStream.minus(d: SampleStream): SampleStream = diff(this, d)

operator fun SampleStream.plus(d: SampleStream): SampleStream = sum(this, d)

fun diff(x: SampleStream, y: SampleStream, shift: Int = 0) = MergedSampleStream(
        x,
        y,
        MergedSampleStreamParams(shift, DiffOperation)
)

fun sum(x: SampleStream, y: SampleStream, shift: Int = 0) = MergedSampleStream(
        x,
        y,
        MergedSampleStreamParams(shift, SumOperation)
)

@Serializer(forClass = MergedStreamOperation::class)
object MergedStreamOperationSerializer {

    override val descriptor: SerialDescriptor
        get() = StringDescriptor.withName("mergeOperation")

    override fun deserialize(decoder: Decoder): MergedStreamOperation =
            when (val operation = decoder.decodeString()) {
                "+" -> SumOperation
                "-" -> DiffOperation
                else -> throw UnsupportedOperationException("`$operation` is not supported")
            }

    override fun serialize(encoder: Encoder, obj: MergedStreamOperation) =
            when (obj) {
                is DiffOperation -> encoder.encodeString("-")
                is SumOperation -> encoder.encodeString("+")
                else -> throw UnsupportedOperationException("${obj::class} is not supported")
            }

}

object SumOperation : MergedStreamOperation {
    override fun apply(a: Sample, b: Sample): Sample = a + b
}

object DiffOperation : MergedStreamOperation {
    override fun apply(a: Sample, b: Sample): Sample = a - b
}

interface MergedStreamOperation {
    fun apply(a: Sample, b: Sample): Sample
}

@Serializable
data class MergedSampleStreamParams(
        val shift: Int,
        @Serializable(with = MergedStreamOperationSerializer::class) val operation: MergedStreamOperation,
        val start: Long = 0,
        val end: Long? = null,
        val timeUnit: TimeUnit = TimeUnit.MILLISECONDS
) : BeanParams()

// TODO that can merge any type of stream in generic way
class MergedSampleStream(
        val sourceStream: SampleStream,
        val mergingStream: SampleStream,
        val params: MergedSampleStreamParams
) : SampleStream, MultiBean<SampleArray, SampleStream>, SinglePartitionBean {

    override val parameters: BeanParams = params

    override val inputs: List<Bean<SampleArray, SampleStream>> = listOf(sourceStream, mergingStream)

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

    override fun asSequence(sampleRate: Float): Sequence<SampleArray> {
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

        val sourceStreamArrayIterator =
                (if (params.start != 0L || params.end != null)
                    sourceStream.rangeProjection(
                            samplesCountToLength(ss, sampleRate, params.timeUnit),
                            l?.plus(ss)?.let { samplesCountToLength(it, sampleRate, params.timeUnit) },
                            params.timeUnit
                    )
                else sourceStream)
                        .asSequence(sampleRate).iterator()

        val mergingStreamArrayIterator =
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

        return object : Iterator<SampleArray> {

            var mergingArrayIdx = 0
            var mergingArray: SampleArray? = null
            var sourceArrayIdx = 0
            var sourceArray: SampleArray? = null

            fun readFromMergingArray(): Sample {
                return if (mergingArrayIdx >= mergingArray?.size ?: 0) { // we need to read next batch
                    if (mergingStreamArrayIterator.hasNext()) { // if there is something to read, then read
                        mergingArray = mergingStreamArrayIterator.next()
                        mergingArrayIdx = 0
                        mergingArray?.get(mergingArrayIdx++) ?: ZeroSample
                    } else { // else simply return zero samples
                        mergingArray = null
                        ZeroSample
                    }
                } else {
                    mergingArray?.get(mergingArrayIdx++) ?: ZeroSample
                }
            }

            fun readFromSourceArray(): Sample {
                return if (sourceArrayIdx >= sourceArray?.size ?: 0) { // we need to read next batch
                    if (sourceStreamArrayIterator.hasNext()) { // if there is something to read, then read
                        sourceArray = sourceStreamArrayIterator.next()
                        sourceArrayIdx = 0
                        sourceArray?.get(sourceArrayIdx++) ?: ZeroSample
                    } else { // else -- no samples left
                        sourceArray = null
                        ZeroSample
                    }
                } else {
                    sourceArray?.get(sourceArrayIdx++) ?: ZeroSample
                }
            }

            override fun hasNext(): Boolean {
                return !(l != null && globalCounter >= l)
            }

            override fun next(): SampleArray {
                return createSampleArray {
                    globalCounter++
                    when {
                        mergingStreamPos < 0 -> {
                            mergingStreamPos++

                            val sourceSample = ZeroSample
                            val mergingSample = readFromMergingArray()

                            params.operation.apply(sourceSample, mergingSample)
                        }
                        mergingStreamPos < newShift -> {
                            mergingStreamPos++
                            sourceStreamPos++

                            val sourceSample = readFromSourceArray()
                            val mergingSample = ZeroSample

                            params.operation.apply(sourceSample, mergingSample)
                        }
                        else -> {
                            mergingStreamPos++
                            sourceStreamPos++

                            val sourceSample = readFromSourceArray()
                            val mergingSample = readFromMergingArray()

                            params.operation.apply(sourceSample, mergingSample)
                        }
                    }
                }
            }
        }.asSequence()
    }
}