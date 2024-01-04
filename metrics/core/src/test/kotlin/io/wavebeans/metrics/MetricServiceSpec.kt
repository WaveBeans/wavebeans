package io.wavebeans.metrics

import assertk.assertThat
import assertk.assertions.isNull
import assertk.assertions.isSuccess
import org.mockito.kotlin.*
import org.mockito.ArgumentMatchers
import org.spekframework.spek2.Spek
import org.spekframework.spek2.lifecycle.CachingMode
import org.spekframework.spek2.style.specification.describe

object MetricServiceSpec : Spek({

    describe("Mock implementation") {

        describe("Increment metric") {

            val myMetricObject by memoized(CachingMode.TEST) { MetricObject.counter("io.wavebeans.execution.metrics", "test", "") }
            val myMetricConnector by memoized(CachingMode.TEST) { mock<MetricConnector>() }

            beforeEachTest {
                MetricService.reset()
                MetricService.registerConnector(myMetricConnector)
            }

            it("should record increment") {
                myMetricObject.increment()
                verify(myMetricConnector).increment(eq(myMetricObject), eq(1.0))
            }

            it("should record increment by 42") {
                myMetricObject.increment(42.0)
                verify(myMetricConnector).increment(eq(myMetricObject), eq(42.0))
            }

            it("should record decrement") {
                myMetricObject.decrement()
                verify(myMetricConnector).decrement(eq(myMetricObject), eq(1.0))
            }

            it("should record decrement by 42") {
                myMetricObject.decrement(42.0)
                verify(myMetricConnector).decrement(eq(myMetricObject), eq(42.0))
            }

            it("shouldn't fail if exception is thrown by one of the connectors during increment") {
                whenever(myMetricConnector.increment(any(), any())).thenThrow(IllegalStateException("shouldn't throw it"))
                assertThat { myMetricObject.increment() }.isSuccess()
                verify(myMetricConnector).increment(eq(myMetricObject), eq(1.0))
            }

            it("shouldn't fail if exception is thrown by one of the connectors during decrement") {
                whenever(myMetricConnector.decrement(any(), any())).thenThrow(IllegalStateException("shouldn't throw it"))
                assertThat { myMetricObject.decrement() }.isSuccess()
                verify(myMetricConnector).decrement(eq(myMetricObject), eq(1.0))
            }
        }

        describe("Gauge metric") {

            val myMetricObject by memoized(CachingMode.TEST) { MetricObject.gauge("io.wavebeans.execution.metrics", "test", "") }
            val myMetricConnector by memoized(CachingMode.TEST) { mock<MetricConnector>() }

            beforeEachTest {
                MetricService.reset()
                MetricService.registerConnector(myMetricConnector)
            }

            it("should record gauge value 42") {
                myMetricObject.set(42.0)
                verify(myMetricConnector).gauge(eq(myMetricObject), eq(42.0))
            }

            it("should record gauge delta by 42") {
                myMetricObject.increment(42.0)
                verify(myMetricConnector).gaugeDelta(eq(myMetricObject), eq(42.0))
            }

            it("shouldn't fail if exception is thrown by one of the connectors during gauge record") {
                whenever(myMetricConnector.gauge(any(), ArgumentMatchers.anyDouble())).thenThrow(IllegalStateException("shouldn't throw it"))
                assertThat { myMetricObject.set(1.0) }.isSuccess()
                verify(myMetricConnector).gauge(eq(myMetricObject), eq(1.0))
            }

            it("shouldn't fail if exception is thrown by one of the connectors during gauge delta record") {
                whenever(myMetricConnector.gaugeDelta(any(), ArgumentMatchers.anyDouble())).thenThrow(IllegalStateException("shouldn't throw it"))
                assertThat { myMetricObject.increment(1.0) }.isSuccess()
                verify(myMetricConnector).gaugeDelta(eq(myMetricObject), eq(1.0))
            }
        }

        describe("Time metric") {

            val myMetricObject by memoized(CachingMode.TEST) { MetricObject.time("io.wavebeans.execution.metrics", "test", "") }
            val myMetricConnector by memoized(CachingMode.TEST) { mock<MetricConnector>() }

            beforeEachTest {
                MetricService.reset()
                MetricService.registerConnector(myMetricConnector)
            }

            it("should record time value 42") {
                myMetricObject.time(42)
                verify(myMetricConnector).time(eq(myMetricObject), eq(42L))
            }

            it("shouldn't fail if exception is thrown by one of the connectors during time record") {
                whenever(myMetricConnector.time(any(), any())).thenThrow(IllegalStateException("shouldn't throw it"))
                assertThat { myMetricObject.time(1L) }.isSuccess()
                verify(myMetricConnector).time(eq(myMetricObject), eq(1L))
            }
        }

    }
})