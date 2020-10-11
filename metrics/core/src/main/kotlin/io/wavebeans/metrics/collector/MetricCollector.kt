package io.wavebeans.metrics.collector

import io.wavebeans.communicator.MetricApiClient
import io.wavebeans.metrics.*
import mu.KotlinLogging
import java.io.Closeable
import java.lang.Thread.sleep
import java.util.concurrent.*
import kotlin.reflect.jvm.jvmName

/**
 * Metric Collector is an implementation of [MetricConnector] that collects values from for
 * the specified [metricObject] locally -- if registered via [MetricService.registerConnector], or remotelly from
 * specified [downstreamCollectors]. The remote connections are established automatically or can be done manually
 * via [MetricCollector.attachCollector] call.
 *
 * The collector may collect only one specified [metricObject] respecting the [MetricObject.tags], the unspecfiued
 * tags are ignored are aggregated together. If the [metricObject] needs ot be collected are verified by
 * [MetricObject.isSuperOf] check.
 */
@Suppress("UNCHECKED_CAST")
abstract class MetricCollector<T : Any>(
        val metricObject: MetricObject<out Any>,
        private val downstreamCollectors: List<String>,
        private val refreshIntervalMs: Long,
        private val granularValueInMs: Long,
        accumulator: (T, T) -> T,
        val serialize: (T) -> String,
        val deserialize: (String) -> T
) : MetricConnector, Closeable {

    companion object {
        private val log = KotlinLogging.logger { }
        private val threadGroup = ThreadGroup(MetricCollector::class.simpleName)
        private val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { Thread(threadGroup, it) }

        init {
            Runtime.getRuntime().addShutdownHook(Thread {
                executor.let {
                    it.shutdown()
                    if (!it.awaitTermination(5000, TimeUnit.MILLISECONDS)) {
                        it.shutdownNow()
                    }
                }
            })
        }
    }

    private val metricCollectorIds = ConcurrentHashMap<String, Long>()

    @Volatile
    private var isAttached: Boolean = false
    @Volatile
    private var isDraining: Boolean = false
    @Volatile
    private var isFinished: Boolean = false
    private val timeseries = TimeseriesList(granularValueInMs, accumulator)
    private val task: ScheduledFuture<*>?

    init {
        if (downstreamCollectors.isNotEmpty() && refreshIntervalMs > 0) {
            task = executor.scheduleAtFixedRate(
                    ::collectFromDownstream,
                    0,
                    refreshIntervalMs,
                    TimeUnit.MILLISECONDS
            )
        } else {
            task = null
        }
    }

    override fun close() {
        task?.let {
            log.info { "Metric collector $this is closing..." }
            isDraining = true
            it.cancel(false)
            val start = System.currentTimeMillis()
            val closeTimeout = 5000
            var closedProperly = false
            while (System.currentTimeMillis() - start < closeTimeout) {
                if (it.isDone && isFinished) {
                    closedProperly = true
                    break
                }
            }
            if (!closedProperly) log.warn {
                "The metric collector $this hasn't been closed properly " +
                        "within $closeTimeout ms. The task is $task"
            } else {
                log.info { "Metric collector $this has closed successfully" }
            }
        }
    }

    override fun increment(metricObject: CounterMetricObject, delta: Double) {
        if (this.metricObject.isSuperOf(metricObject)) {
            timeseries.append(delta as T)
        }
    }

    override fun decrement(metricObject: CounterMetricObject, delta: Double) {
        if (this.metricObject.isSuperOf(metricObject)) {
            timeseries.append(-delta as T)
        }
    }

    override fun gauge(metricObject: GaugeMetricObject, value: Double) {
        if (this.metricObject.isSuperOf(metricObject)) {
            timeseries.append(GaugeAccumulator(true, value) as T)
        }
    }

    override fun gaugeDelta(metricObject: GaugeMetricObject, delta: Double) {
        if (this.metricObject.isSuperOf(metricObject)) {
            timeseries.append(GaugeAccumulator(false, delta) as T)
        }
    }

    override fun time(metricObject: TimeMetricObject, valueInMs: Long) {
        if (this.metricObject.isSuperOf(metricObject)) {
            timeseries.append(TimeAccumulator(1, valueInMs) as T)
        }
    }

    /**
     * Performs the collection of the values from collector. The internal state of the collector
     * is cleaned up up to the [collectUpToTimestamp] time marker. The values are aggregated according to the
     * collector paramteres.
     */
    fun collectValues(collectUpToTimestamp: Long): List<TimedValue<T>> {
        return timeseries.removeAllBefore(collectUpToTimestamp)
    }

    /**
     * Merges the collector internal value with specified sequence of [TimedValue]s. The specified values will
     * be aggregated according to the parameters specified by the collector.
     */
    fun merge(with: Sequence<TimedValue<T>>) {
        timeseries.merge(with)
    }

    /**
     * Tries to attach the collector to downstream ones via [MetricApiClient.attachCollector]. All attach attempts should be
     * performed successfully in order for this method to return true. During attempt it doens't try to connect once
     * again to collectors which are already connected.
     *
     * @return true if eventually all downstream collectors are connected, otherwise false.
     */
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

    /**
     * Synchronously awaits attaching to the downstream collector.
     *
     * @param timeout amount of time to spend waiting.
     *
     * @return true if collector has been attached within timeout, otherwise false.
     */
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

    /**
     * Represents the current attachement state to the downstream collectors. Does not perform the actual check.
     */
    fun isAttached(): Boolean = isAttached

    /**
     * Performs the collection of the metrics from all downstream collectors. If not all collectors are attached
     * performs the attach first. The method call do not fail on [Exception].
     */
    internal fun collectFromDownstream(collectUpToTimestamp: Long = System.currentTimeMillis()) {
        if (!isDraining && !isFinished) {
            val start = System.currentTimeMillis()
            if (isAttached) {
                try {
                    mergeWithDownstreamCollectors(collectUpToTimestamp)
                    log.trace { "Merged $metricObject with downstream on $downstreamCollectors. Took ${System.currentTimeMillis() - start}ms" }
                    if (isDraining) isFinished = true
                } catch (e: Exception) {
                    log.warn(e) { "Error merging $metricObject with downstream on $downstreamCollectors. Took ${System.currentTimeMillis() - start}ms" }
                }
            } else {
                log.info { "Attempting attach $metricObject to $downstreamCollectors..." }
                if (attachCollector()) {
                    isAttached = true
                    log.info { "Attached $metricObject to $downstreamCollectors" }
                } else {
                    log.info { "Couldn't attach $metricObject to $downstreamCollectors" }
                }
            }
        }
    }

    /**
     * Performs the merge with the downstream provider. One by one calls the [MetricApiClient.collectValues] to
     * [MetricCollector.collectValues] from the downstream collectors to [merge] them after into the collector
     * current state.
     */
    internal fun mergeWithDownstreamCollectors(collectUpToTimestamp: Long) {
        downstreamCollectors.forEach { location ->
            MetricApiClient(location).use { metricApiClient ->
                merge(
                        metricApiClient.collectValues(
                                collectUpToTimestamp,
                                metricCollectorIds.getValue(location)
                        ).map { deserialize(it.serializedValue) at it.timestamp }
                )
            }
        }
    }

    override fun toString(): String {
        return "MetricCollector(metricObject=$metricObject, downstreamCollectors=$downstreamCollectors, refreshIntervalMs=$refreshIntervalMs, granularValueInMs=$granularValueInMs)"
    }
}

