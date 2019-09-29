package mux.lib.stream

import kotlinx.serialization.Serializable
import mux.lib.*
import mux.lib.MuxNode
import java.util.concurrent.TimeUnit

fun SampleStream.trim(length: Long, timeUnit: TimeUnit = TimeUnit.MILLISECONDS): FiniteSampleStream =
        TrimmedFiniteSampleStream(this, TrimmedFiniteSampleStreamParams(length, timeUnit))

@Serializable
data class TrimmedFiniteSampleStreamParams(
        val length: Long,
        val timeUnit: TimeUnit
) : MuxParams()

class TrimmedFiniteSampleStream(
        val sampleStream: SampleStream,
        val params: TrimmedFiniteSampleStreamParams
) : FiniteSampleStream, AlterMuxNode<Sample, SampleStream, Sample, FiniteSampleStream> {

    override val parameters: MuxParams = params

    override val input: MuxNode<Sample, SampleStream> = sampleStream

    override fun asSequence(sampleRate: Float): Sequence<Sample> =
            sampleStream
                    .asSequence(sampleRate)
                    .take(timeToSampleIndexFloor(TimeUnit.NANOSECONDS.convert(params.length, params.timeUnit), TimeUnit.NANOSECONDS, sampleRate).toInt())

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): TrimmedFiniteSampleStream =
            TrimmedFiniteSampleStream(
                    sampleStream.rangeProjection(start, end, timeUnit),
                    params.copy(
                            length = end?.minus(start) ?: (timeUnit.convert(params.length, params.timeUnit) - start)
                    )
            )

    override fun length(timeUnit: TimeUnit): Long = timeUnit.convert(params.length, params.timeUnit)

}
