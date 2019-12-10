package io.wavebeans.lib

import java.math.RoundingMode
import java.util.concurrent.TimeUnit

/**
 * Interface marks the bean as it can be used for time measures.
 */
interface TimeBeanStream<T : Any> : BeanStream<T> {
    /**
     * Gets the sample index of time point within specific sample rate.
     *
     * @param sampleRate sample rate to use for calculations
     * @param time desired time point to convert
     * @param timeUnit [TimeUnit] of the [time] point
     * @param roundingMode as calculation is approximate which rounding methods to use.
     *        Generally, [RoundingMode.FLOOR] is used for calculating starting index,
     *        [RoundingMode.CEILING] is used for calculating end index. So these two are supported only now.
     */
    fun timeToSampleIndex(sampleRate: Float, time: Long, timeUnit: TimeUnit, roundingMode: RoundingMode): Long

}

/**
 * [TimeBeanStream] implementation for [Sample] case.
 */
interface SampleTimeBeanStream : TimeBeanStream<Sample> {

    override fun timeToSampleIndex(sampleRate: Float, time: Long, timeUnit: TimeUnit, roundingMode: RoundingMode): Long =
            timeToSampleIndex(time, timeUnit, sampleRate, roundingMode)

}