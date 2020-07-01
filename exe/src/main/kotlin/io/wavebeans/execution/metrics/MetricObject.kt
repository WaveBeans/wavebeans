package io.wavebeans.execution.metrics

data class MetricObject(
        /**
         * The component of the metric, i.e. `io.wavebeans.execution.metrics`
         */
        val component: String,
        /**
         * The specific name of the metric, i.e. `requestCount`
         */
        val name: String,
        /**
         * The tags of the metrics
         */
        val tags: Map<String, String> = emptyMap()
) {

    fun addTags(vararg newTags: Pair<String, String>) = MetricObject(component, name, tags + newTags)

    /**
     * Calls [MetricConnector.increment] on [MetricServer].
     */
    fun increment(delta: Long = 1L) = MetricServer.increment(this, delta)

    /**
     * Calls [MetricConnector.decrement] on [MetricServer].
     */
    fun decrement(delta: Long = 1L) = MetricServer.decrement(this, delta)

    /**
     * Calls [MetricConnector.gauge] on [MetricServer].
     */
    fun gauge(value: Long) = MetricServer.gauge(this, value)

    /**
     * Calls [MetricConnector.gauge] on [MetricServer].
     */
    fun gauge(value: Double) = MetricServer.gauge(this, value)

    /**
     * Calls [MetricConnector.gaugeDelta] on [MetricServer].
     */
    fun gaugeDelta(delta: Long) = MetricServer.gaugeDelta(this, delta)

    /**
     * Calls [MetricConnector.gaugeDelta] on [MetricServer].
     */
    fun gaugeDelta(delta: Double) = MetricServer.gaugeDelta(this, delta)

    /**
     * Calls [MetricConnector.time] on [MetricServer].
     */
    fun time(valueInMs: Long) = MetricServer.time(this, valueInMs)
}