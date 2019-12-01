package io.wavebeans.lib.stream.fft

import io.wavebeans.lib.*
import kotlinx.serialization.Serializable
import java.util.concurrent.TimeUnit

fun FftStream.trim(length: Long, timeUnit: TimeUnit = TimeUnit.MILLISECONDS): FiniteFftStream =
        TrimmedFiniteFftStream(this, TrimmedFiniteFftStreamParams(length, timeUnit))

@Serializable
data class TrimmedFiniteFftStreamParams(
        val length: Long,
        val timeUnit: TimeUnit
) : BeanParams()

class TrimmedFiniteFftStream(
        val fftStream: FftStream,
        val params: TrimmedFiniteFftStreamParams
) : FiniteFftStream, AlterBean<FftSample, FftStream, FftSample, FiniteFftStream>, SinglePartitionBean {

    override val parameters: BeanParams = params

    override val input: Bean<FftSample, FftStream> = fftStream

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
                    params.copy(
                            length = end?.minus(start) ?: (params.timeUnit.convert(params.length, timeUnit) - start)
                    )
            )

    override fun length(timeUnit: TimeUnit): Long = timeUnit.convert(params.length, params.timeUnit)

}
