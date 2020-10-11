package io.wavebeans.metrics

interface MetricConnector {
    /**
     * Increments the specified counter by delta.
     *
     * @param metricObject the [MetricObject] to record the value for.
     * @param delta the value of increment, by default 1.
     */
    fun increment(metricObject: CounterMetricObject, delta: Double = 1.0)

    /**
     * Decrements the specified counter by delta.
     *
     * @param metricObject the [MetricObject] to record the value for.
     * @param delta the value of increment, by default 1.
     */
    fun decrement(metricObject: CounterMetricObject, delta: Double = 1.0)

    /**
     * Records the latest fixed value for the specified gauge.
     *
     * @param metricObject the [MetricObject] to record the value for.
     * @param value the new reading of the gauge.
     */
    fun gauge(metricObject: GaugeMetricObject, value: Double)

    /**
     * Records a change in the value for the specified gauge.
     *
     * @param metricObject the [MetricObject] to record the value for.
     * @param delta the delta to apply to the gauge (positive or negative).
     */
    fun gaugeDelta(metricObject: GaugeMetricObject, delta: Double)

    /**
     * Records an execution time in milliseconds.
     *
     * @param metricObject the [MetricObject] to record the value for.
     * @param valueInMs the time value in milliseconds.
     */
    fun time(metricObject: TimeMetricObject, valueInMs: Long)
}