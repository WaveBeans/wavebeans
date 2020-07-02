package io.wavebeans.execution.metrics

import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object MetricServiceSpec : Spek({

    describe("Mock implementation") {

        val myMetricObject = MetricObject("io.wavebeans.execution.metrics", "test", emptyMap())
        val myMetricConnector = mock<MetricConnector>()

        beforeGroup {
            MetricServer.metricConnectors += myMetricConnector
        }

        it("should record increment") {
            myMetricObject.increment()
            verify(myMetricConnector).increment(eq(myMetricObject), eq(1L))
        }

        it("should record increment by 42") {
            myMetricObject.increment(42)
            verify(myMetricConnector).increment(eq(myMetricObject), eq(42L))
        }

        it("should record decrement") {
            myMetricObject.decrement()
            verify(myMetricConnector).decrement(eq(myMetricObject), eq(1L))
        }

        it("should record decrement by 42") {
            myMetricObject.decrement(42)
            verify(myMetricConnector).decrement(eq(myMetricObject), eq(42L))
        }

        it("should record gauge value 42") {
            myMetricObject.gauge(42)
            verify(myMetricConnector).gauge(eq(myMetricObject), eq(42L))
        }

        it("should record gauge value 42.0") {
            myMetricObject.gauge(42.0)
            verify(myMetricConnector).gauge(eq(myMetricObject), eq(42.0))
        }

        it("should record gauge delta by 42") {
            myMetricObject.gaugeDelta(42)
            verify(myMetricConnector).gaugeDelta(eq(myMetricObject), eq(42L))
        }

        it("should record gauge delta by 42.0") {
            myMetricObject.gaugeDelta(42.0)
            verify(myMetricConnector).gaugeDelta(eq(myMetricObject), eq(42.0))
        }

        it("should record time value 42") {
            myMetricObject.time(42)
            verify(myMetricConnector).time(eq(myMetricObject), eq(42L))
        }
    }
})