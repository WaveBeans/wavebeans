package io.wavebeans.metrics.prometheus

import io.prometheus.client.*
import io.prometheus.client.exporter.HTTPServer
import io.wavebeans.metrics.*
import io.wavebeans.metrics.MetricConnector
import io.wavebeans.metrics.MetricObject
import java.io.Closeable
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap


/**
 * Implementation connector for Prometheus.
 *
 * NOTE: Metric and label names may have limited characters, everything illegal is automatically
 * replaced with underscore symbol no conform rules. Colon `:` used to separate component and name.
 */
class PrometheusMetricConnector(
        port: Int?,
        private val registry: CollectorRegistry = CollectorRegistry.defaultRegistry
) : MetricConnector, Closeable {

    private val server: HTTPServer? = port?.let { HTTPServer(InetSocketAddress(it), registry, true) }

    private val prometheusMetrics = ConcurrentHashMap<MetricObject<*>, Array<Collector>>()

    private val invalidCharactersRegex = "[^a-zA-Z0-9_:]".toRegex()

    override fun increment(metricObject: CounterMetricObject, delta: Double) {
        val c = prometheusMetrics.computeIfAbsent(metricObject.withoutTags()) {
            arrayOf(
                    Gauge.build(prepareMetricName(metricObject), metricObject.description)
                            .labelNames(*prepareLabelNames(metricObject))
                            .register(registry)
            )
        }
        (c[0] as Gauge).labels(*prepareLabels(metricObject)).inc(delta)
    }

    override fun decrement(metricObject: CounterMetricObject, delta: Double) {
        increment(metricObject, -delta)
    }

    override fun gauge(metricObject: GaugeMetricObject, value: Double) {
        val c = prometheusMetrics.computeIfAbsent(metricObject.withoutTags()) {
            arrayOf(
                    Gauge.build(prepareMetricName(metricObject), metricObject.description)
                            .labelNames(*prepareLabelNames(metricObject))
                            .register(registry)
            )
        }
        (c[0] as Gauge).labels(*prepareLabels(metricObject)).set(value)
    }

    override fun gaugeDelta(metricObject: GaugeMetricObject, delta: Double) {
        val c = prometheusMetrics.computeIfAbsent(metricObject.withoutTags()) {
            arrayOf(
                    Gauge.build(prepareMetricName(metricObject), metricObject.description)
                            .labelNames(*prepareLabelNames(metricObject))
                            .register(registry)
            )
        }
        (c[0] as Gauge).labels(*prepareLabels(metricObject)).inc(delta)
    }

    override fun time(metricObject: TimeMetricObject, valueInMs: Long) {
        val c = prometheusMetrics.computeIfAbsent(metricObject.withoutTags()) {
            arrayOf(
                    Summary.build(prepareMetricName(metricObject), metricObject.description)
                            .quantile(0.99, 0.01)
                            .quantile(0.95, 0.02)
                            .quantile(0.50, 0.05)
                            .labelNames(*prepareLabelNames(metricObject))
                            .register(registry)
            )
        }
        (c[0] as Summary).labels(*prepareLabels(metricObject)).observe(valueInMs.toDouble())
    }

    override fun close() {
        server?.stop()
    }

    private fun prepareLabels(metricObject: MetricObject<*>) =
            metricObject.possibleTags.map { metricObject.tags[it] ?: "n/a" }.toTypedArray()

    private fun prepareLabelNames(metricObject: MetricObject<*>) =
            metricObject.possibleTags.map { it.replace(invalidCharactersRegex, "_") }.toTypedArray()

    private fun prepareMetricName(metricObject: MetricObject<*>) =
            metricObject.component.replace(invalidCharactersRegex, "_") +
                    ":" + metricObject.name.replace(invalidCharactersRegex, "_")
}
