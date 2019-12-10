package io.wavebeans.lib.stream

import io.wavebeans.lib.*
import kotlinx.serialization.Serializable
import java.util.concurrent.TimeUnit

fun BeanStream<Sample>.trim(length: Long, timeUnit: TimeUnit = TimeUnit.MILLISECONDS): FiniteSampleStream =
        TrimmedFiniteSampleStream(this, TrimmedFiniteSampleStreamParams(length, timeUnit))

@Serializable
data class TrimmedFiniteSampleStreamParams(
        val length: Long,
        val timeUnit: TimeUnit
) : BeanParams()

// TODO move this functionality to output and perhaps get rid of FiniteSampleStream. This functionality looks fishy overall
class TrimmedFiniteSampleStream(
        val sampleStream: BeanStream<Sample>,
        val params: TrimmedFiniteSampleStreamParams
) : FiniteSampleStream, SingleBean<Sample>, SinglePartitionBean, SampleTimeBeanStream {

    override val parameters: BeanParams = params

    override val input: Bean<Sample> = sampleStream

    override fun asSequence(sampleRate: Float): Sequence<Sample> {
        var samplesToTake = timeToSampleIndexFloor(TimeUnit.NANOSECONDS.convert(params.length, params.timeUnit), TimeUnit.NANOSECONDS, sampleRate)
        val iterator = sampleStream.asSequence(sampleRate).iterator()

        return object : Iterator<Sample> {

            override fun hasNext(): Boolean =
                    samplesToTake > 0 && iterator.hasNext()

            override fun next(): Sample {
                samplesToTake--
                return iterator.next()
            }

        }.asSequence()
    }

    override fun length(timeUnit: TimeUnit): Long = timeUnit.convert(params.length, params.timeUnit)

}
