package io.wavebeans.lib.stream

import io.wavebeans.lib.*
import kotlinx.serialization.Serializable
import java.util.concurrent.TimeUnit

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
) : FiniteStream<T>, SingleBean<T>, SinglePartitionBean {

    override fun asSequence(sampleRate: Float): Sequence<T> {
        var samplesToTake = timeToSampleIndexFloor(TimeUnit.NANOSECONDS.convert(parameters.length, parameters.timeUnit), TimeUnit.NANOSECONDS, sampleRate)
        val iterator = input.asSequence(sampleRate).iterator()

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

}
