package mux.lib.stream

import mux.lib.*
import java.util.concurrent.TimeUnit

fun FftStream.trim(length: Long, timeUnit: TimeUnit = TimeUnit.MILLISECONDS): FiniteFftStream = TrimmedFiniteFftStream(this, length, timeUnit)

class TrimmedFiniteFftStream(
        val fftStream: FftStream,
        val length: Long,
        val timeUnit: TimeUnit
) : FiniteFftStream, AlterMuxNode<FftSample, FftStream, FftSample, FiniteFftStream> {

    override val input: MuxNode<FftSample, FftStream> = fftStream

    override fun asSequence(sampleRate: Float): Sequence<FftSample> =
            fftStream
                    .asSequence(sampleRate)
                    .take(
                            fftStream.estimateFftSamplesCount(
                                    timeToSampleIndexFloor(this.length(TimeUnit.NANOSECONDS), TimeUnit.NANOSECONDS, sampleRate)
                            ).toInt()
                    )

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): FiniteFftStream =
            TrimmedFiniteFftStream(
                    fftStream.rangeProjection(start, end, timeUnit),
                    end?.minus(start) ?: (this.timeUnit.convert(length, timeUnit) - start),
                    timeUnit
            )

    override fun length(timeUnit: TimeUnit): Long = timeUnit.convert(length, this.timeUnit)

}
