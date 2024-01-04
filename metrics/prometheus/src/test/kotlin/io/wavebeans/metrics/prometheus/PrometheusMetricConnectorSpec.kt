package io.wavebeans.metrics.prometheus

import assertk.Assert
import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.prop
import org.mockito.kotlin.*
import io.prometheus.client.Collector
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Gauge
import io.prometheus.client.Summary
import io.wavebeans.metrics.MetricObject
import io.wavebeans.metrics.MetricService
import org.spekframework.spek2.Spek
import org.spekframework.spek2.lifecycle.CachingMode
import org.spekframework.spek2.style.specification.describe

object PrometheusMetricConnectorSpec : Spek({

    val registry by memoized(CachingMode.TEST) {
        val registry = mock<CollectorRegistry>()
        registry
    }

    val connector by memoized(CachingMode.TEST) {
        val connector = PrometheusMetricConnector(null, registry)
        connector
    }

    beforeEachTest {
        MetricService.reset()
        MetricService.registerConnector(connector)
    }

    afterEachTest {
        connector.close()
    }

    describe("Counter") {
        val metricCounter = MetricObject.counter(
                "my_test_metric",
                "counter",
                "something",
                "label1", "label2"
        )

        it("should increment without tags") {
            metricCounter.increment(1.0)
            val counter = registry.real<Gauge>()
            assertThat(counter.labels("n/a", "n/a").get()).isEqualTo(1.0)

            metricCounter.increment(1.0)
            verify(registry, only()).register(any())
            assertThat(counter.labels("n/a", "n/a").get()).isEqualTo(2.0)
        }

        it("should increment with tags") {
            metricCounter.withTags("label2" to "v").increment(1.0)
            val counter = registry.real<Gauge>()
            assertThat(counter.labels("n/a", "v").get()).isEqualTo(1.0)

            metricCounter.withTags("label2" to "v").increment(1.0)
            verify(registry, only()).register(any())
            assertThat(counter.labels("n/a", "v").get()).isEqualTo(2.0)
        }

        it("should decrement without tags") {
            metricCounter.decrement(1.0)
            val counter = registry.real<Gauge>()
            assertThat(counter.labels("n/a", "n/a").get()).isEqualTo(-1.0)

            metricCounter.decrement(1.0)
            verify(registry, only()).register(any())
            assertThat(counter.labels("n/a", "n/a").get()).isEqualTo(-2.0)
        }

        it("should decrement with tags") {
            metricCounter.withTags("label2" to "v").decrement(1.0)
            val counter = registry.real<Gauge>()
            assertThat(counter.labels("n/a", "v").get()).isEqualTo(-1.0)

            metricCounter.withTags("label2" to "v").decrement(1.0)
            verify(registry, only()).register(any())
            assertThat(counter.labels("n/a", "v").get()).isEqualTo(-2.0)
        }
    }

    describe("Gauge") {
        val metricCounter = MetricObject.gauge(
                "my_test_metric",
                "counter",
                "something",
                "label1", "label2"
        )

        it("should set without tags") {
            metricCounter.set(1.0)
            val counter = registry.real<Gauge>()
            assertThat(counter.labels("n/a", "n/a").get()).isEqualTo(1.0)

            metricCounter.set(2.0)
            verify(registry, only()).register(any())
            assertThat(counter.labels("n/a", "n/a").get()).isEqualTo(2.0)
        }

        it("should set with tags") {
            metricCounter.withTags("label2" to "v").set(1.0)
            val counter = registry.real<Gauge>()
            assertThat(counter.labels("n/a", "v").get()).isEqualTo(1.0)

            metricCounter.withTags("label2" to "v").set(2.0)
            verify(registry, only()).register(any())
            assertThat(counter.labels("n/a", "v").get()).isEqualTo(2.0)
        }

        it("should increment without tags") {
            metricCounter.set(1.0)
            metricCounter.increment(2.0)
            val counter = registry.real<Gauge>()
            assertThat(counter.labels("n/a", "n/a").get()).isEqualTo(3.0)

            metricCounter.increment(3.0)
            verify(registry, only()).register(any())
            assertThat(counter.labels("n/a", "n/a").get()).isEqualTo(6.0)
        }

        it("should increment with tags") {
            metricCounter.withTags("label2" to "v").set(1.0)
            metricCounter.withTags("label2" to "v").increment(2.0)
            val counter = registry.real<Gauge>()
            assertThat(counter.labels("n/a", "v").get()).isEqualTo(3.0)

            metricCounter.withTags("label2" to "v").increment(3.0)
            verify(registry, only()).register(any())
            assertThat(counter.labels("n/a", "v").get()).isEqualTo(6.0)
        }

        it("should decrement without tags") {
            metricCounter.set(6.0)
            metricCounter.decrement(2.0)
            val counter = registry.real<Gauge>()
            assertThat(counter.labels("n/a", "n/a").get()).isEqualTo(4.0)

            metricCounter.decrement(3.0)
            verify(registry, only()).register(any())
            assertThat(counter.labels("n/a", "n/a").get()).isEqualTo(1.0)
        }

        it("should decrement without tags") {
            metricCounter.withTags("label2" to "v").set(6.0)
            metricCounter.withTags("label2" to "v").decrement(2.0)
            val counter = registry.real<Gauge>()
            assertThat(counter.labels("n/a", "v").get()).isEqualTo(4.0)

            metricCounter.withTags("label2" to "v").decrement(3.0)
            verify(registry, only()).register(any())
            assertThat(counter.labels("n/a", "v").get()).isEqualTo(1.0)
        }
    }

    describe("Time") {
        val metricCounter = MetricObject.time(
                "my_test_metric",
                "counter",
                "something",
                "label1", "label2"
        )

        it("should add time without tags") {
            metricCounter.time(50)
            val counter = registry.real<Summary>()
            assertThat(counter.labels("n/a", "n/a").get()).all {
                count().isEqualTo(1.0)
                sum().isEqualTo(50.0)
            }

            metricCounter.time(40)
            verify(registry, only()).register(any())
            assertThat(counter.labels("n/a", "n/a").get()).all {
                count().isEqualTo(2.0)
                sum().isEqualTo(90.0)
            }
        }

        it("should add time with tags") {
            metricCounter.withTags("label2" to "v").time(50)
            val counter = registry.real<Summary>()
            assertThat(counter.labels("n/a", "v").get()).all {
                count().isEqualTo(1.0)
                sum().isEqualTo(50.0)
            }

            metricCounter.withTags("label2" to "v").time(40)
            verify(registry, only()).register(any())
            assertThat(counter.labels("n/a", "v").get()).all {
                count().isEqualTo(2.0)
                sum().isEqualTo(90.0)
            }
        }
    }
})

private fun Assert<Summary.Child.Value>.sum() = prop("sum") { it.sum }

private fun Assert<Summary.Child.Value>.count() = prop("count") { it.count }

private inline fun <reified T : Collector> CollectorRegistry.real(): T {
    val captor = argumentCaptor<T>()
    verify(this).register(captor.capture())
    assertThat(captor.allValues.size).isEqualTo(1)
    return captor.firstValue
}