package io.wavebeans.lib.stream

import io.wavebeans.lib.BeanParams
import io.wavebeans.lib.BeanStream
import io.wavebeans.lib.SingleBean
import io.wavebeans.lib.SinglePartitionBean
import io.wavebeans.lib.TimeUnit
import io.wavebeans.lib.timeToSampleIndexFloor
import kotlinx.serialization.Serializable
import kotlin.properties.Delegates

/**
 * Trims the current `BeanStream` to a specified duration, resulting in a `FiniteStream`.
 *
 * @param length the duration to trim the stream to, expressed in the specified time unit.
 * @param timeUnit the time unit for the given length, defaults to `TimeUnit.MILLISECONDS`.
 * @return a `FiniteStream` limited to the specified duration.
 */
fun <T:Any> BeanStream<T>.trim(length: Long, timeUnit: TimeUnit = TimeUnit.MILLISECONDS): FiniteStream<T> =
        TrimmedFiniteStream(this, TrimmedFiniteSampleStreamParams(length, timeUnit))

@Serializable
data class TrimmedFiniteSampleStreamParams(
        val length: Long,
        val timeUnit: TimeUnit
) : BeanParams

// TODO move this functionality to output and perhaps get rid of FiniteSampleStream. This functionality looks fishy overall
class TrimmedFiniteStream<T : Any>(
        override val input: BeanStream<T>,
        override val parameters: TrimmedFiniteSampleStreamParams
) : AbstractOperationBeanStream<T, T>(input), FiniteStream<T>, SingleBean<T>, SinglePartitionBean {

    var outputSampleCount by Delegates.notNull<Long>()

    override fun operationSequence(input: Sequence<T>, sampleRate: Float): Sequence<T> {
        outputSampleCount = timeToSampleIndexFloor(
                TimeUnit.NANOSECONDS.convert(parameters.length, parameters.timeUnit),
                TimeUnit.NANOSECONDS,
                sampleRate
        )
        var samplesToTake = outputSampleCount
        val iterator = input.iterator()

        return object : Iterator<T> {

            override fun hasNext(): Boolean =
                    samplesToTake > 0 && iterator.hasNext()

            override fun next(): T {
                val next = iterator.next()
                samplesToTake -= SampleCountMeasurement.samplesInObject(next)
                return next
            }

        }.asSequence()
    }

    override fun length(timeUnit: TimeUnit): Long = timeUnit.convert(parameters.length, parameters.timeUnit)

    override fun samplesCount(): Long = outputSampleCount

}
