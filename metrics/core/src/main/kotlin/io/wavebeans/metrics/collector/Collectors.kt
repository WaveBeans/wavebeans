package io.wavebeans.metrics.collector

import io.wavebeans.metrics.CounterMetricObject
import io.wavebeans.metrics.GaugeMetricObject
import io.wavebeans.metrics.TimeMetricObject

fun TimeMetricObject.collector(
        downstreamCollectors: List<String> = emptyList(),
        refreshIntervalMs: Long = 5000,
        granularValueInMs: Long = 60000
): MetricCollector<TimeMetricObject, TimeAccumulator> {
    return MetricCollector(
            this,
            downstreamCollectors,
            refreshIntervalMs,
            granularValueInMs,
            accumulator = { a, b -> a + b },
            serialize = { "${it.count}:${it.time}" },
            deserialize = { it.split(":", limit = 2).let { TimeAccumulator(it[0].toInt(), it[1].toLong()) } }
    )
}

fun CounterMetricObject.collector(
        downstreamCollectors: List<String> = emptyList(),
        refreshIntervalMs: Long = 5000,
        granularValueInMs: Long = 60000
): MetricCollector<CounterMetricObject, Double> {
    return MetricCollector(
            this,
            downstreamCollectors,
            refreshIntervalMs,
            granularValueInMs,
            accumulator = { a, b -> a + b },
            serialize = { it.toString() },
            deserialize = { it.toDouble() }
    )
}

fun GaugeMetricObject.collector(
        downstreamCollectors: List<String> = emptyList(),
        refreshIntervalMs: Long = 5000,
        granularValueInMs: Long = 60000
): MetricCollector<GaugeMetricObject, GaugeAccumulator> {
    return MetricCollector(
            this,
            downstreamCollectors,
            refreshIntervalMs,
            granularValueInMs,
            accumulator = { a, b -> a + b },
            serialize = { "${it.isSet}:${it.increment}" },
            deserialize = { it.split(":", limit = 2).let { GaugeAccumulator(it[0].toBoolean(), it[1].toDouble()) } }
    )
}
