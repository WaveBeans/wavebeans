package io.wavebeans.metrics.collector

import io.wavebeans.communicator.MetricApiClient
import io.wavebeans.metrics.*
import mu.KotlinLogging
import java.io.Closeable
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.reflect.jvm.jvmName

@Suppress("UNCHECKED_CAST")
abstract class MetricCollector<M : MetricObject<*>, T : Any>(
        val metricObject: M,
        private val downstreamCollectors: List<String>,
        private val refreshIntervalMs: Long,
        private val granularValueInMs: Long,
        accumulator: (T, T) -> T,
        val serialize: (T) -> String,
        val deserialize: (String) -> T
) : MetricConnector, Closeable {

    companion object {
        private val log = KotlinLogging.logger { }
    }

    private val threadGroup = ThreadGroup(this::class.simpleName + "@${refreshIntervalMs}ms")
    private var executor: ScheduledExecutorService? = null
    private var isAttached: Boolean = false

    init {
        if (downstreamCollectors.isNotEmpty()) {
            executor = Executors.newSingleThreadScheduledExecutor { Thread(threadGroup, it) }
            executor?.scheduleAtFixedRate(
                    {
                        val start = System.currentTimeMillis()
                        if (isAttached) {
                            try {
                                mergeWithDownstreamCollectors(System.currentTimeMillis())
                                log.debug { "Merged $metricObject with downstream on $downstreamCollectors. Took ${System.currentTimeMillis() - start}ms" }
                            } catch (e: Exception) {
                                log.warn(e) { "Error merging $metricObject with downstream on $downstreamCollectors. Took ${System.currentTimeMillis() - start}ms" }
                            }
                        } else {
                            log.info { "Attempting attach $metricObject on $downstreamCollectors..." }
                            if (attachCollector()) isAttached = true
                        }
                    },
                    0,
                    refreshIntervalMs,
                    TimeUnit.MILLISECONDS
            )
        }
    }

    private val timeseries = TimeseriesList(granularValueInMs, accumulator)

    override fun increment(metricObject: CounterMetricObject, delta: Double) {
        if (metricObject.isLike(this.metricObject)) {
            val r = timeseries.append(delta as T)
            log.debug { "$metricObject increment delta=$delta: $r, ${timeseries.valuesCopy()}" }
        }
    }

    override fun decrement(metricObject: CounterMetricObject, delta: Double) {
        if (metricObject.isLike(this.metricObject)) {
            timeseries.append(-delta as T)
        }
    }

    override fun gauge(metricObject: GaugeMetricObject, value: Double) {
        if (metricObject.isLike(this.metricObject)) {
            timeseries.append(GaugeAccumulator(true, value) as T)
        }
    }

    override fun gaugeDelta(metricObject: GaugeMetricObject, delta: Double) {
        if (metricObject.isLike(this.metricObject)) {
            timeseries.append(GaugeAccumulator(false, delta) as T)
        }
    }

    override fun time(metricObject: TimeMetricObject, valueInMs: Long) {
        if (metricObject.isLike(this.metricObject)) {
            timeseries.append(TimeAccumulator(1, valueInMs) as T)
        }
    }

    fun collectValues(collectUpToTimestamp: Long): List<TimedValue<T>> {
        return timeseries.removeAllBefore(collectUpToTimestamp)
    }

    fun merge(with: Sequence<TimedValue<T>>) {
        timeseries.merge(with)
    }

    fun mergeWithDownstreamCollectors(collectUpToTimestamp: Long) {
        downstreamCollectors.forEach { location ->
            MetricApiClient(location).use { metricApiClient ->
                merge(
                        metricApiClient.collectValues<M, T>(
                                collectUpToTimestamp,
                                metricObject.type,
                                metricObject.name,
                                metricObject.component,
                                metricObject.tags
                        ).map { deserialize(it.serializedValue) at it.timestamp }
                )
            }
        }
    }

    private fun attachCollector(): Boolean {
        return downstreamCollectors.all { location ->
            MetricApiClient(location).use { metricApiClient ->
                try {
                    metricApiClient.attachCollector(
                            this::class.jvmName,
                            emptyList(),
                            metricObject.type,
                            metricObject.name,
                            metricObject.component,
                            metricObject.tags,
                            refreshIntervalMs,
                            granularValueInMs
                    )
                    log.info { "Attached to collector of $metricObject on $location" }
                    true
                } catch (e: Exception) {
                    log.info(e) { "Can't attach collector of $metricObject on $location" }
                    false
                }
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


