package io.wavebeans.execution.metrics

import mu.KotlinLogging

object MetricService : MetricConnector {

    private val log = KotlinLogging.logger { }

    private val metricConnectors: MutableList<MetricConnector> = mutableListOf()

    fun reset(): MetricService {
        log.debug { "Resetting metric service from:\n${Thread.currentThread().stackTrace.joinToString("\n", postfix = "\n----end of stackTrace----") { "\t at $it" }}" }
        metricConnectors.clear()
        return this
    }

    fun registerConnector(connector: MetricConnector): MetricService {
        log.debug { "Registering new connector from:\n${Thread.currentThread().stackTrace.joinToString("\n", postfix = "\n----end of stackTrace----") { "\t at $it" }}" }
        metricConnectors += connector
        return this
    }

    override fun increment(metricObject: MetricObject, delta: Long) {
        metricConnectors.forEach {
            try {
                it.increment(metricObject, delta)
            } catch (e: Throwable) {
                log.warn(e) { "Can't increment by $delta for $metricObject on connector $it" }
            }
        }
    }

    override fun decrement(metricObject: MetricObject, delta: Long) {
        metricConnectors.forEach {
            try {
                it.decrement(metricObject, delta)
            } catch (e: Throwable) {
                log.warn(e) { "Can't decrement by $delta for $metricObject on connector $it" }
            }
        }
    }

    override fun gauge(metricObject: MetricObject, value: Long) {
        metricConnectors.forEach {
            try {
                it.gauge(metricObject, value)
            } catch (e: Throwable) {
                log.warn(e) { "Can't gauge value $value for $metricObject on connector $it" }
            }
        }
    }

    override fun gauge(metricObject: MetricObject, value: Double) {
        metricConnectors.forEach {
            try {
                it.gauge(metricObject, value)
            } catch (e: Throwable) {
                log.warn(e) { "Can't gauge value $value for $metricObject on connector $it" }
            }
        }
    }

    override fun gaugeDelta(metricObject: MetricObject, delta: Long) {
        metricConnectors.forEach {
            try {
                it.gaugeDelta(metricObject, delta)
            } catch (e: Throwable) {
                log.warn(e) { "Can't gauge dekta $delta for $metricObject on connector $it" }
            }

        }
    }

    override fun gaugeDelta(metricObject: MetricObject, delta: Double) {
        metricConnectors.forEach {
            try {
                it.gaugeDelta(metricObject, delta)
            } catch (e: Throwable) {
                log.warn(e) { "Can't gauge delta $delta for $metricObject on connector $it" }
            }
        }
    }

    override fun time(metricObject: MetricObject, valueInMs: Long) {
        metricConnectors.forEach {
            try {
                it.time(metricObject, valueInMs)
            } catch (e: Throwable) {
                log.warn(e) { "Can't record time $valueInMs for $metricObject on connector $it" }
            }
        }
    }
}
