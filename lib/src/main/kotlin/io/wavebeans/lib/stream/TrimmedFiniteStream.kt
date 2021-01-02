package io.wavebeans.lib.stream

import io.wavebeans.lib.*
import kotlinx.serialization.Serializable
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

fun <T:Any> BeanStream<T>.trim(length: Long, timeUnit: TimeUnit = TimeUnit.MILLISECONDS): FiniteStream<T> =
        TrimmedFiniteStream(this, TrimmedFiniteSampleStreamParams(length, timeUnit))

@Serializable
data class TrimmedFiniteSampleStreamParams(
        val length: Long,
        val timeUnit: TimeUnit
) : BeanParams()

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
