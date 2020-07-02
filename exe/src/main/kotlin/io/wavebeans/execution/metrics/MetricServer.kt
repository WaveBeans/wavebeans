package io.wavebeans.execution.metrics

object MetricServer : MetricConnector {

    val metricConnectors: MutableList<MetricConnector> = mutableListOf()

    override fun increment(metricObject: MetricObject, delta: Long) {
        metricConnectors.forEach { it.increment(metricObject, delta) }
    }

    override fun decrement(metricObject: MetricObject, delta: Long) {
        metricConnectors.forEach { it.decrement(metricObject, delta) }
    }

    override fun gauge(metricObject: MetricObject, value: Long) {
        metricConnectors.forEach { it.gauge(metricObject, value) }
    }

    override fun gauge(metricObject: MetricObject, value: Double) {
        metricConnectors.forEach { it.gauge(metricObject, value) }
    }

    override fun gaugeDelta(metricObject: MetricObject, delta: Long) {
        metricConnectors.forEach { it.gaugeDelta(metricObject, delta) }
    }

    override fun gaugeDelta(metricObject: MetricObject, delta: Double) {
        metricConnectors.forEach { it.gaugeDelta(metricObject, delta) }
    }

    override fun time(metricObject: MetricObject, valueInMs: Long) {
        metricConnectors.forEach { it.time(metricObject, valueInMs) }
    }
}
