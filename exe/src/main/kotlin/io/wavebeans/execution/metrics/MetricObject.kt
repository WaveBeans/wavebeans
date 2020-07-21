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

    fun withTags(vararg newTags: Pair<String, String>): MetricObject = MetricObject(component, name, tags + newTags)

    fun withoutTags(): MetricObject = if (tags.isEmpty()) this else MetricObject(component, name)

    /**
     * Compares another [MetricObject] ignoring tags
     */
    fun isLike(another: MetricObject): Boolean = component == another.component && name == another.name

    /**
     * Calls [MetricConnector.increment] on [MetricService].
     */
    fun increment(delta: Long = 1L) = MetricService.increment(this, delta)

    /**
     * Calls [MetricConnector.decrement] on [MetricService].
     */
    fun decrement(delta: Long = 1L) = MetricService.decrement(this, delta)

    /**
     * Calls [MetricConnector.gauge] on [MetricService].
     */
    fun gauge(value: Long) = MetricService.gauge(this, value)

    /**
     * Calls [MetricConnector.gauge] on [MetricService].
     */
    fun gauge(value: Double) = MetricService.gauge(this, value)

    /**
     * Calls [MetricConnector.gaugeDelta] on [MetricService].
     */
    fun gaugeDelta(delta: Long) = MetricService.gaugeDelta(this, delta)

    /**
     * Calls [MetricConnector.gaugeDelta] on [MetricService].
     */
    fun gaugeDelta(delta: Double) = MetricService.gaugeDelta(this, delta)

    /**
     * Calls [MetricConnector.time] on [MetricService].
     */
    fun time(valueInMs: Long) = MetricService.time(this, valueInMs)
}