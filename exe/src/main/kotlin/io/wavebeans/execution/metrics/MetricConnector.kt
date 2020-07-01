package io.wavebeans.execution.metrics

interface MetricConnector {
    /**
     * Increments the specified counter by delta.
     *
     * @param metricObject the [MetricObject] to record the value for.
     * @param delta the value of increment, by default 1.
     */
    fun increment(metricObject: MetricObject, delta: Long = 1L)

    /**
     * Decrements the specified counter by delta.
     *
     * @param metricObject the [MetricObject] to record the value for.
     * @param delta the value of increment, by default 1.
     */
    fun decrement(metricObject: MetricObject, delta: Long = 1L)

    /**
     * Records the latest fixed value for the specified gauge.
     *
     * @param metricObject the [MetricObject] to record the value for.
     * @param value the value to record.
     */
    fun gauge(metricObject: MetricObject, value: Long)

    /**
     * Records the latest fixed value for the specified gauge.
     *
     * @param metricObject the [MetricObject] to record the value for.
     * @param value the new reading of the gauge.
     */
    fun gauge(metricObject: MetricObject, value: Double)

    /**
     * Records a change in the value for the specified gauge.
     *
     * @param metricObject the [MetricObject] to record the value for.
     * @param delta the delta to apply to the gauge (positive or negative).
     */
    fun gaugeDelta(metricObject: MetricObject, delta: Long)

    /**
     * Records a change in the value for the specified gauge.
     *
     * @param metricObject the [MetricObject] to record the value for.
     * @param delta the delta to apply to the gauge (positive or negative).
     */
    fun gaugeDelta(metricObject: MetricObject, delta: Double)

    /**
     * Records an execution time in milliseconds.
     *
     * @param metricObject the [MetricObject] to record the value for.
     * @param valueInMs the time value in milliseconds.
     */
    fun time(metricObject: MetricObject, valueInMs: Long)
}