package io.wavebeans.metrics.collector

import io.wavebeans.communicator.MetricApiClient
import io.wavebeans.metrics.*
import mu.KotlinLogging
import java.io.Closeable
import java.lang.Thread.sleep
import java.util.concurrent.ConcurrentHashMap
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
    private val metricCollectorIds = ConcurrentHashMap<String, Long>()

    @Volatile
    private var isAttached: Boolean = false
    private val timeseries = TimeseriesList(granularValueInMs, accumulator)

    init {
        if (downstreamCollectors.isNotEmpty() && refreshIntervalMs > 0) {
            executor = Executors.newSingleThreadScheduledExecutor { Thread(threadGroup, it) }
            executor?.scheduleAtFixedRate(
                    ::collectFromDownstream,
                    0,
                    refreshIntervalMs,
                    TimeUnit.MILLISECONDS
            )
        }
    }

    internal fun collectFromDownstream(collectUpToTimestamp: Long = System.currentTimeMillis()) {
        val start = System.currentTimeMillis()
        if (isAttached) {
            try {
                mergeWithDownstreamCollectors(collectUpToTimestamp)
                log.debug { "Merged $metricObject with downstream on $downstreamCollectors. Took ${System.currentTimeMillis() - start}ms" }
            } catch (e: Exception) {
                log.warn(e) { "Error merging $metricObject with downstream on $downstreamCollectors. Took ${System.currentTimeMillis() - start}ms" }
            }
        } else {
            log.info { "Attempting attach $metricObject on $downstreamCollectors..." }
            if (attachCollector()) isAttached = true
        }
    }

    fun awaitAttached(timeout: Long = 1000): Boolean {
        val start = System.currentTimeMillis()
        while (!this.isAttached) {
            if (System.currentTimeMillis() - start < timeout)
                sleep(0)
            else
                return false
        }
        return true
    }

    fun isAttached(): Boolean = isAttached

    override fun increment(metricObject: CounterMetricObject, delta: Double) {
        if (metricObject.isLike(this.metricObject)) {
            timeseries.append(delta as T)
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
                                metricCollectorIds.getValue(location)
                        ).map { deserialize(it.serializedValue) at it.timestamp }
                )
            }
        }
    }

    fun attachCollector(): Boolean {
        return downstreamCollectors.all { location ->
            if (!metricCollectorIds.containsKey(location)) {
                MetricApiClient(location).use { metricApiClient ->
                    try {
                        val collectorId = metricApiClient.attachCollector(
                                this::class.jvmName,
                                emptyList(),
                                metricObject.type,
                                metricObject.name,
                                metricObject.component,
                                metricObject.tags,
                                refreshIntervalMs,
                                granularValueInMs
                        )
                        metricCollectorIds[location] = collectorId
                        log.info { "Attached to collector of $metricObject on $location under id $collectorId" }
                        true
                    } catch (e: Exception) {
                        log.info(e) { "Can't attach collector of $metricObject on $location" }
                        false
                    }
                }
            } else {
                true
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


