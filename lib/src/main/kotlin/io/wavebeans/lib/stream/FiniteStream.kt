package io.wavebeans.lib.stream

import io.wavebeans.lib.BeanStream
import java.util.concurrent.TimeUnit

/**
 * A stream of finite length that extends the capabilities of a [BeanStream].
 * Unlike potentially infinite streams, this stream defines a predetermined length
 * measured in a specified time unit and provides the total number of samples.
 *
 * @param T the type of the data objects being streamed.
 */
interface FiniteStream<T : Any> : BeanStream<T> {

    fun length(timeUnit: TimeUnit = TimeUnit.MILLISECONDS): Long

    fun samplesCount(): Long
}
