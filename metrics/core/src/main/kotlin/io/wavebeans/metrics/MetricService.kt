package io.wavebeans.metrics

import mu.KotlinLogging

object MetricService : MetricConnector {

    private val log = KotlinLogging.logger { }

    private val metricConnectors: MutableList<MetricConnector> = mutableListOf()

    fun reset(): MetricService {
        log.debug { "Resetting metric service from:\n${Thread.currentThread().stackTrace.drop(4).joinToString("\n", postfix = "\n----end of stackTrace----") { "\t at $it" }}" }
        metricConnectors.clear()
        return this
    }

    fun registerConnector(connector: MetricConnector): MetricService {
        metricConnectors += connector
        log.info {
            "Registered new connector $connector" +
                    if (log.isDebugEnabled) " from:\n${Thread.currentThread().stackTrace.drop(4).joinToString("\n", postfix = "\n----end of stackTrace----") { "\t at $it" }}"
                    else ""
        }
        return this
    }

    override fun increment(metricObject: CounterMetricObject, delta: Double) {
        metricConnectors.forEach {
            try {
                it.increment(metricObject, delta)
            } catch (e: Throwable) {
                log.warn(e) { "Can't increment by $delta for $metricObject on connector $it" }
            }
        }
    }

    override fun decrement(metricObject: CounterMetricObject, delta: Double) {
        metricConnectors.forEach {
            try {
                it.decrement(metricObject, delta)
            } catch (e: Throwable) {
                log.warn(e) { "Can't decrement by $delta for $metricObject on connector $it" }
            }
        }
    }


    override fun gauge(metricObject: GaugeMetricObject, value: Double) {
        metricConnectors.forEach {
            try {
                it.gauge(metricObject, value)
            } catch (e: Throwable) {
                log.warn(e) { "Can't gauge value $value for $metricObject on connector $it" }
            }
        }
    }


    override fun gaugeDelta(metricObject: GaugeMetricObject, delta: Double) {
        metricConnectors.forEach {
            try {
                it.gaugeDelta(metricObject, delta)
            } catch (e: Throwable) {
                log.warn(e) { "Can't gauge delta $delta for $metricObject on connector $it" }
            }
        }
    }

    override fun time(metricObject: TimeMetricObject, valueInMs: Long) {
        metricConnectors.forEach {
            try {
                it.time(metricObject, valueInMs)
            } catch (e: Throwable) {
                log.warn(e) { "Can't record time $valueInMs for $metricObject on connector $it" }
            }
        }
    }
}
