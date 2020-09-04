package io.wavebeans.metrics

import kotlin.reflect.jvm.jvmName

data class GaugeMetricObject(
        override val component: String,
        override val name: String,
        override val description: String,
        override val possibleTags: List<String>,
        override val tags: Map<String, String> = emptyMap(),
        override val type: String = GaugeMetricObject::class.jvmName
) : MetricObject<GaugeMetricObject> {

    override fun withTags(vararg newTags: Pair<String, String>): GaugeMetricObject = GaugeMetricObject(component, name, description, possibleTags, tags + newTags)

    override fun withoutTags(): GaugeMetricObject = if (tags.isEmpty()) this else GaugeMetricObject(component, name, description, possibleTags)

    /**
     * Calls [MetricConnector.gauge] on [MetricService].
     */
    fun set(value: Double) = MetricService.gauge(this, value)

    /**
     * Calls [MetricConnector.gaugeDelta] on [MetricService].
     */
    fun increment(delta: Double) = MetricService.gaugeDelta(this, delta)

    /**
     * Calls [MetricConnector.gaugeDelta] on [MetricService].
     */
    fun decrement(delta: Double) = MetricService.gaugeDelta(this, -delta)
}