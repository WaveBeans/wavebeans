package io.wavebeans.metrics.collector

import io.wavebeans.communicator.MetricApiClient
import io.wavebeans.metrics.*
import java.io.Closeable
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

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

@Suppress("UNCHECKED_CAST")
class MetricCollector<M : MetricObject<*>, T : Any>(
        val metricObject: M,
        private val downstreamCollectors: List<String>,
        refreshIntervalMs: Long,
        granularValueInMs: Long,
        accumulator: (T, T) -> T,
        val serialize: (T) -> String,
        val deserialize: (String) -> T
) : MetricConnector, Closeable {

    private val threadGroup = ThreadGroup(this::class.simpleName + "@${refreshIntervalMs}ms")
    private var executor: ScheduledExecutorService? = null

    init {
        if (downstreamCollectors.isNotEmpty()) {
            executor = Executors.newSingleThreadScheduledExecutor { Thread(threadGroup, it) }
            var lastCollectionTimestamp = System.currentTimeMillis()
            executor?.scheduleAtFixedRate(
                    {
                        mergeWithDownstreamCollectors(lastCollectionTimestamp)
                        lastCollectionTimestamp = System.currentTimeMillis()
                    },
                    0,
                    refreshIntervalMs,
                    TimeUnit.MILLISECONDS
            )
        }
    }

    private val timeseries = TimeseriesList(granularValueInMs, accumulator)

    override fun increment(metricObject: CounterMetricObject, delta: Double) {
        timeseries.append(delta as T)
    }

    override fun decrement(metricObject: CounterMetricObject, delta: Double) {
        timeseries.append(-delta as T)
    }

    override fun gauge(metricObject: GaugeMetricObject, value: Double) {
        TODO("Not yet implemented")
    }

    override fun gaugeDelta(metricObject: GaugeMetricObject, delta: Double) {
        TODO("Not yet implemented")
    }

    override fun time(metricObject: TimeMetricObject, valueInMs: Long) {
        timeseries.append(TimeAccumulator(1, valueInMs) as T)
    }

    fun collectValues(lastCollectionTimestamp: Long): List<TimedValue<T>> {
        return timeseries.removeAllBefore(lastCollectionTimestamp)
    }

    fun merge(with: Sequence<TimedValue<T>>) {
        timeseries.merge(with)
    }

    fun mergeWithDownstreamCollectors(lastCollectionTimestamp: Long) {
        downstreamCollectors.forEach { location ->
            MetricApiClient(location).use { metricApiClient ->
                merge(
                        metricApiClient.collectValues<M, T>(
                                lastCollectionTimestamp,
                                metricObject.name,
                                metricObject.component,
                                metricObject.tags
                        ).map { deserialize(it.serializedValue) at it.timestamp }
                )
            }
        }
    }

    override fun close() {
        executor?.let {
            it.shutdown()
            if (!it.awaitTermination(5000, TimeUnit.MILLISECONDS)) {
                it.shutdownNow()
            }
        }
    }
}


