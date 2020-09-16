package io.wavebeans.metrics.collector

import io.wavebeans.metrics.CounterMetricObject
import io.wavebeans.metrics.GaugeMetricObject
import io.wavebeans.metrics.MetricObject
import io.wavebeans.metrics.TimeMetricObject
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmName

fun createCollector(
        collectorClass: String,
        metricObject: MetricObject<*>,
        downstreamCollectors: List<String>,
        refreshIntervalMs: Long,
        granularValueInMs: Long
): MetricCollector<Any> {
    val constructorParameters = arrayOf(MetricObject::class, List::class, Long::class, Long::class)
    val collectorConstructor = try {
        Class.forName(collectorClass)
    } catch (e: ClassNotFoundException) {
        throw IllegalStateException("`$collectorClass` is not found", e)
    }
            .constructors
            .firstOrNull {
                it.parameterTypes
                        .zip(constructorParameters.indices)
                        .all { (clazz, idx) ->
                            clazz.kotlin.isSubclassOf(constructorParameters[idx])
                        }
            }
            ?: throw IllegalStateException("Can't create collector of class $collectorClass " +
                    "because required constructor is not found, expected constructor with following parameters: " +
                    constructorParameters.joinToString(",") { it.jvmName })

    try {
        @Suppress("UNCHECKED_CAST")
        return collectorConstructor.newInstance(
                metricObject,
                downstreamCollectors,
                refreshIntervalMs,
                granularValueInMs
        ) as MetricCollector<Any>
    } catch (e: Exception) {
        throw IllegalStateException("Can't create the collector with " +
                "collectorClass=$collectorClass(constructor=$collectorConstructor)," +
                " metricObject=$metricObject, downstreamCollectors=$downstreamCollectors, " +
                "refreshIntervalMs=$refreshIntervalMs, granularValueInMs=$granularValueInMs", e)
    }
}

class TimeMetricCollector(
        metricObject: TimeMetricObject,
        downstreamCollectors: List<String> = emptyList(),
        refreshIntervalMs: Long = 5000,
        granularValueInMs: Long = 60000
) : MetricCollector<TimeAccumulator>(
        metricObject,
        downstreamCollectors,
        refreshIntervalMs,
        granularValueInMs,
        accumulator = { a, b -> a + b },
        serialize = { "${it.count}:${it.time}" },
        deserialize = { it.split(":", limit = 2).let { TimeAccumulator(it[0].toInt(), it[1].toLong()) } }

)

fun TimeMetricObject.collector(
        downstreamCollectors: List<String> = emptyList(),
        refreshIntervalMs: Long = 5000,
        granularValueInMs: Long = 60000
): MetricCollector<TimeAccumulator> {
    return TimeMetricCollector(
            this,
            downstreamCollectors,
            refreshIntervalMs,
            granularValueInMs
    )
}

class CounterMetricCollector(
        metricObject: CounterMetricObject,
        downstreamCollectors: List<String> = emptyList(),
        refreshIntervalMs: Long = 5000,
        granularValueInMs: Long = 60000
) : MetricCollector<Double>(
        metricObject,
        downstreamCollectors,
        refreshIntervalMs,
        granularValueInMs,
        accumulator = { a, b -> a + b },
        serialize = { it.toString() },
        deserialize = { it.toDouble() }

)

fun CounterMetricObject.collector(
        downstreamCollectors: List<String> = emptyList(),
        refreshIntervalMs: Long = 5000,
        granularValueInMs: Long = 60000
): MetricCollector<Double> {
    return CounterMetricCollector(
            this,
            downstreamCollectors,
            refreshIntervalMs,
            granularValueInMs
    )
}

class GaugeMetricCollector(
        metricObject: GaugeMetricObject,
        downstreamCollectors: List<String> = emptyList(),
        refreshIntervalMs: Long = 5000,
        granularValueInMs: Long = 60000
) : MetricCollector<GaugeAccumulator>(
        metricObject,
        downstreamCollectors,
        refreshIntervalMs,
        granularValueInMs,
        accumulator = { a, b -> a + b },
        serialize = { "${it.isSet}:${it.increment}" },
        deserialize = { it.split(":", limit = 2).let { GaugeAccumulator(it[0].toBoolean(), it[1].toDouble()) } }

)


fun GaugeMetricObject.collector(
        downstreamCollectors: List<String> = emptyList(),
        refreshIntervalMs: Long = 5000,
        granularValueInMs: Long = 60000
): MetricCollector<GaugeAccumulator> {
    return GaugeMetricCollector(
            this,
            downstreamCollectors,
            refreshIntervalMs,
            granularValueInMs
    )
}
