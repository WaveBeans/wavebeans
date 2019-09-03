package mux.lib.stream

import mux.lib.Sample
import mux.lib.timeToSampleIndexFloor
import java.util.concurrent.TimeUnit

fun SampleStream.trim(length: Long, timeUnit: TimeUnit = TimeUnit.MILLISECONDS): FiniteStream = TrimmedFiniteStream(this, length, timeUnit)

class TrimmedFiniteStream(
        val sampleStream: SampleStream,
        val length: Long,
        val timeUnit: TimeUnit
) : FiniteStream {

    override fun asSequence(sampleRate: Float): Sequence<Sample> =
            sampleStream
                    .asSequence(sampleRate)
                    .take(timeToSampleIndexFloor(this.length(TimeUnit.NANOSECONDS), TimeUnit.NANOSECONDS, sampleRate).toInt())

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): TrimmedFiniteStream =
            TrimmedFiniteStream(
                    sampleStream.rangeProjection(start, end, timeUnit),
                    end?.minus(start) ?: (this.timeUnit.convert(length, timeUnit) - start),
                    timeUnit
            )

    override fun length(timeUnit: TimeUnit): Long = this.timeUnit.convert(length, timeUnit)

}
