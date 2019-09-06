package mux.lib.stream

import mux.lib.*
import java.util.concurrent.TimeUnit
import kotlin.math.max

fun diff(x: SampleStream, y: SampleStream, shift: Int = 0) = MergedSampleStream(x, y, shift, { a, b -> a - b })

fun sum(x: SampleStream, y: SampleStream, shift: Int = 0) = MergedSampleStream(x, y, shift, { a, b -> a + b })

class MergedSampleStream(
        val sourceStream: SampleStream,
        val mergingStream: SampleStream,
        val shift: Int,
        val operation: (Sample, Sample) -> Sample,
        val start: Long = 0,
        val end: Long? = null,
        val timeUnit: TimeUnit = TimeUnit.MILLISECONDS
) : SampleStream {

    override fun mux(): MuxNode = MuxMultiInputNode(Mux("MergedSampleStream"), listOf(sourceStream.mux(), mergingStream.mux()))

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): SampleStream {
        return MergedSampleStream(
                sourceStream,
                mergingStream,
                shift,
                operation,
                start + this.start,
                end?.let { it + (this.end ?: 0) },
                timeUnit
        )
    }

    override fun asSequence(sampleRate: Float): Sequence<Sample> {
        val s = max(timeToSampleIndexFloor(start, timeUnit, sampleRate), 0L)
        val e = end?.let { timeToSampleIndexCeil(it, timeUnit, sampleRate) }
        val l = e?.minus(s)

        var ss: Long
        var ms: Long
        var newShift: Long
        if (shift < 0) {
            ss = s + shift
            if (ss < 0) ss = 0
            ms = s
            newShift = shift.toLong() + s
            if (newShift > 0) newShift = 0L
        } else {
            ss = s
            ms = s - shift
            if (ms < 0) ms = 0
            newShift = shift - s
            if (newShift < 0) newShift = 0L
        }

        val sourceStreamIterator =
                (if (start != 0L || end != null)
                    sourceStream.rangeProjection(
                            samplesCountToLength(ss, sampleRate, timeUnit),
                            l?.plus(ss)?.let { samplesCountToLength(it, sampleRate, timeUnit) },
                            timeUnit
                    )
                else sourceStream)
                        .asSequence(sampleRate).iterator()
        val mergingStreamIterator =
                (if (start != 0L || end != null)
                    mergingStream.rangeProjection(
                            samplesCountToLength(ms, sampleRate, timeUnit),
                            l?.plus(ms)?.let { samplesCountToLength(it, sampleRate, timeUnit) },
                            timeUnit
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

                        operation(sourceSample, mergingSample)
                    }
                    mergingStreamPos < newShift -> {
                        mergingStreamPos++
                        sourceStreamPos++

                        val sourceSample =
                                if (sourceStreamIterator.hasNext()) sourceStreamIterator.next()
                                else ZeroSample
                        val mergingSample = ZeroSample

                        operation(sourceSample, mergingSample)
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

                        operation(sourceSample, mergingSample)
                    }
                }
            }
        }.asSequence()
    }
}
