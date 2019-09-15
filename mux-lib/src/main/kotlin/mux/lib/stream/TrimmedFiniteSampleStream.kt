package mux.lib.stream

import mux.lib.*
import mux.lib.Mux
import mux.lib.MuxNode
import mux.lib.SingleMuxNode
import java.util.concurrent.TimeUnit

fun SampleStream.trim(length: Long, timeUnit: TimeUnit = TimeUnit.MILLISECONDS): FiniteSampleStream = TrimmedFiniteSampleStream(this, length, timeUnit)

class TrimmedFiniteSampleStream(
        val sampleStream: SampleStream,
        val length: Long,
        val timeUnit: TimeUnit
) : FiniteSampleStream, AlterMuxNode<Sample, SampleStream, Sample, FiniteSampleStream> {

    override val input: MuxNode<Sample, SampleStream> = sampleStream

    override fun asSequence(sampleRate: Float): Sequence<Sample> =
            sampleStream
                    .asSequence(sampleRate)
                    .take(timeToSampleIndexFloor(TimeUnit.NANOSECONDS.convert(length, timeUnit), TimeUnit.NANOSECONDS, sampleRate).toInt())

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): TrimmedFiniteSampleStream =
            TrimmedFiniteSampleStream(
                    sampleStream.rangeProjection(start, end, timeUnit),
                    end?.minus(start) ?: (this.timeUnit.convert(length, timeUnit) - start),
                    timeUnit
            )

    override fun length(timeUnit: TimeUnit): Long = timeUnit.convert(length, this.timeUnit)

}
