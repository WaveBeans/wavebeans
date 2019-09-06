package mux.lib.stream

import mux.lib.*
import java.util.concurrent.TimeUnit

fun SampleStream.trim(length: Long, timeUnit: TimeUnit = TimeUnit.MILLISECONDS): FiniteSampleStream = TrimmedFiniteSampleStream(this, length, timeUnit)

class TrimmedFiniteSampleStream(
        val sampleStream: SampleStream,
        val length: Long,
        val timeUnit: TimeUnit
) : FiniteSampleStream {

    override fun mux(): MuxNode = MuxSingleInputNode(Mux("TrimmedFiniteSampleStream(len=$length)"), sampleStream.mux())

    override fun asSequence(sampleRate: Float): Sequence<Sample> =
            sampleStream
                    .asSequence(sampleRate)
                    .take(timeToSampleIndexFloor(this.length(TimeUnit.NANOSECONDS), TimeUnit.NANOSECONDS, sampleRate).toInt())

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): TrimmedFiniteSampleStream =
            TrimmedFiniteSampleStream(
                    sampleStream.rangeProjection(start, end, timeUnit),
                    end?.minus(start) ?: (this.timeUnit.convert(length, timeUnit) - start),
                    timeUnit
            )

    override fun length(timeUnit: TimeUnit): Long = this.timeUnit.convert(length, timeUnit)

}
