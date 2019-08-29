package mux.lib.stream

import mux.lib.Sample
import mux.lib.ZeroSample
import mux.lib.timeToSampleIndexCeil
import mux.lib.timeToSampleIndexFloor
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

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): SampleStream {
        return MergedSampleStream(sourceStream, mergingStream, shift, operation, start, end, timeUnit)
    }

    override fun asSequence(sampleRate: Float): Sequence<Sample> {
        val startIndex = max(timeToSampleIndexFloor(start, timeUnit, sampleRate).toInt(), 0)
        val endIndex = end?.let { timeToSampleIndexCeil(it, timeUnit, sampleRate).toInt() }

        return object : Sequence<Sample> {

            override fun iterator(): Iterator<Sample> {

                val sourceStreamIterator =
                        (if (start != 0L) sourceStream.rangeProjection(start, end, timeUnit) else sourceStream)
                                .asSequence(sampleRate).iterator()
                val mergingStreamIterator =
                        (if (start != 0L) mergingStream.rangeProjection(start, end, timeUnit) else mergingStream)
                                .asSequence(sampleRate).iterator()

                var globalCounter = startIndex.toLong()
                var sourceStreamPos = 0L
                var mergingStreamPos = if (shift < 0) shift.toLong() else 0L


                return object : Iterator<Sample> {

                    override fun hasNext(): Boolean {
                        return !(endIndex != null && globalCounter >= endIndex)
                    }

                    override fun next(): Sample {
                        globalCounter++
                        return when {
                            mergingStreamPos < 0 -> {
                                mergingStreamPos++
                                if (mergingStreamIterator.hasNext()) mergingStreamIterator.next() else ZeroSample
                            }
                            mergingStreamPos < shift -> {
                                mergingStreamPos++
                                sourceStreamPos++
                                if (sourceStreamIterator.hasNext()) sourceStreamIterator.next() else ZeroSample
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
                }
            }
        }
    }
}
