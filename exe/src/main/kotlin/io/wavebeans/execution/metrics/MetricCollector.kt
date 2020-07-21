package io.wavebeans.execution.metrics

import io.wavebeans.communicator.MetricApiClient
import java.io.Closeable
import java.util.concurrent.*

class MetricCollector(
        private val downstreamCollectors: List<String> = emptyList(),
        refreshIntervalMs: Long = 5000,
        private val granularValueInMs: Long = 60000
) : MetricConnector, Closeable {

    private val threadGroup = ThreadGroup(this::class.simpleName + "@${refreshIntervalMs}ms")
    private var executor: ScheduledExecutorService? = null
    private val started = CountDownLatch(1)

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

    private val timeseries = ConcurrentHashMap<MetricObject, TimeseriesList<Double>>()

    override fun increment(metricObject: MetricObject, delta: Long) {
        val timeseriesList = timeseries.computeIfAbsent(metricObject.withoutTags()) { TimeseriesList(granularValueInMs) { a, b -> a + b } }
        timeseriesList.append(delta.toDouble())
    }

    override fun decrement(metricObject: MetricObject, delta: Long) {
        val timeseriesList = timeseries.computeIfAbsent(metricObject.withoutTags()) { TimeseriesList(granularValueInMs) { a, b -> a + b } }
        timeseriesList.append(-delta.toDouble())
    }

    override fun gauge(metricObject: MetricObject, value: Long) {
        TODO("Not yet implemented")
    }

    override fun gauge(metricObject: MetricObject, value: Double) {
        TODO("Not yet implemented")
    }

    override fun gaugeDelta(metricObject: MetricObject, delta: Long) {
        TODO("Not yet implemented")
    }

    override fun gaugeDelta(metricObject: MetricObject, delta: Double) {
        TODO("Not yet implemented")
    }

    override fun time(metricObject: MetricObject, valueInMs: Long) {
        val timeseriesList = timeseries.computeIfAbsent(metricObject.withoutTags()) { TimeseriesList(granularValueInMs) { a, b -> a + b } }
        timeseriesList.append(valueInMs.toDouble())
    }

    fun collectValues(lastCollectionTimestamp: Long): List<Pair<MetricObject, List<Pair<Long, Double>>>> {
        return timeseries.entries.map { (key, value) ->
            key.withoutTags() to value.removeAllBefore(lastCollectionTimestamp)
        }
    }

    fun merge(with: Sequence<Pair<MetricObject, List<Pair<Long, Double>>>>) {
        with.forEach { (metricObject, values) ->
            val timeseriesList = timeseries.computeIfAbsent(metricObject.withoutTags()) { TimeseriesList(granularValueInMs) { a, b -> a + b } }
            timeseriesList.merge(values)
        }
    }

    fun mergeWithDownstreamCollectors(lastCollectionTimestamp: Long) {
        downstreamCollectors.forEach { location ->
            MetricApiClient(location).use { metricApiClient ->
                merge(with = metricApiClient.collectValues(lastCollectionTimestamp) { MetricObject(it.component, it.name, it.tagsMap) })
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


