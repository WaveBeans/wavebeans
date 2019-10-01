package mux.lib

import java.util.concurrent.TimeUnit

interface BeanStream<T : Any, S : Any> : Bean<T, S> {

    /**
     *  Gets the input as a sequence of samples.
     *
     *  @param sampleRate sample rate to use for generating sequence. If sample rate is different
     *          from the source iy should be resampled, or an exception should be thrown if it's not supported
     *  @throws UnsupportedOperationException if input doesn't support resampling
     **/
    fun asSequence(sampleRate: Float): Sequence<T>

    /**
     * Gets a projection of the object in the specified time range.
     *
     * @param start starting point of the projection in time units.
     * @param end ending point of the projection (including) in time units. Null if till the end
     * @param timeUnit the units the projection is defined in (i.e seconds, milliseconds, microseconds). TODO: replace TimeUnit with non-java.util.concurrent one
     *
     * @return the projection of specific time interval
     */
    fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit = TimeUnit.MILLISECONDS): S
}