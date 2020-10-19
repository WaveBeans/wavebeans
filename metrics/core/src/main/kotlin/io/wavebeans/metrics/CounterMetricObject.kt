package io.wavebeans.metrics

import kotlin.reflect.jvm.jvmName

data class CounterMetricObject(
        override val component: String,
        override val name: String,
        override val description: String,
        override val possibleTags: List<String>,
        override val tags: Map<String, String> = emptyMap(),
        override val type: String = CounterMetricObject::class.jvmName
) : MetricObject<CounterMetricObject> {

    override fun withTags(vararg newTags: Pair<String, String>): CounterMetricObject = CounterMetricObject(component, name, description, possibleTags, tags + newTags)

    override fun withoutTags(): CounterMetricObject = if (tags.isEmpty()) this else CounterMetricObject(component, name, description, possibleTags)

    /**
     * Calls [MetricConnector.increment] on [MetricService].
     */
    fun increment(delta: Double = 1.0) = MetricService.increment(this, delta)

    /**
     * Calls [MetricConnector.decrement] on [MetricService].
     */
    fun decrement(delta: Double = 1.0) = MetricService.decrement(this, delta)
}