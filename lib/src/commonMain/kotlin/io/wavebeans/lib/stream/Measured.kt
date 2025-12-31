package io.wavebeans.lib.stream

/**
 * Classes implementing that interface are marked as measurable by time-to-sample functions like projection() and trim().
 * Should implement the method that measures internal state in samples.
 */
interface Measured {
    /**
     * Returns number of samples in the object, to be used in conjunction with [SampleCountMeasurement]
     */
    fun measure(): Int
}
