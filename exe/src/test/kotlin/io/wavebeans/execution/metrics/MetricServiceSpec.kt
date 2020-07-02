package io.wavebeans.execution.metrics

import assertk.assertThat
import assertk.assertions.isNull
import assertk.catch
import com.nhaarman.mockitokotlin2.*
import org.mockito.ArgumentMatchers
import org.spekframework.spek2.Spek
import org.spekframework.spek2.lifecycle.CachingMode
import org.spekframework.spek2.style.specification.describe

object MetricServiceSpec : Spek({

    describe("Mock implementation") {

        val myMetricObject by memoized(CachingMode.TEST) { MetricObject("io.wavebeans.execution.metrics", "test", emptyMap()) }
        val myMetricConnector by memoized(CachingMode.TEST) { mock<MetricConnector>() }

        beforeEachTest {
            MetricService.reset()
            MetricService.registerConnector(myMetricConnector)
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

        it("shouldn't fail if exception is thrown by one of the connectors during increment") {
            whenever(myMetricConnector.increment(any(), any())).thenThrow(IllegalStateException("shouldn't throw it"))
            assertThat(catch { myMetricObject.increment() }).isNull()
            verify(myMetricConnector).increment(eq(myMetricObject), eq(1L))
        }

        it("shouldn't fail if exception is thrown by one of the connectors during decrement") {
            whenever(myMetricConnector.decrement(any(), any())).thenThrow(IllegalStateException("shouldn't throw it"))
            assertThat(catch { myMetricObject.decrement() }).isNull()
            verify(myMetricConnector).decrement(eq(myMetricObject), eq(1L))
        }

        it("shouldn't fail if exception is thrown by one of the connectors during gauge record with long") {
            whenever(myMetricConnector.gauge(any(), ArgumentMatchers.anyLong())).thenThrow(IllegalStateException("shouldn't throw it"))
            assertThat(catch { myMetricObject.gauge(1L) }).isNull()
            verify(myMetricConnector).gauge(eq(myMetricObject), eq(1L))
        }

        it("shouldn't fail if exception is thrown by one of the connectors during gauge record with double") {
            whenever(myMetricConnector.gauge(any(), ArgumentMatchers.anyDouble())).thenThrow(IllegalStateException("shouldn't throw it"))
            assertThat(catch { myMetricObject.gauge(1.0) }).isNull()
            verify(myMetricConnector).gauge(eq(myMetricObject), eq(1.0))
        }

        it("shouldn't fail if exception is thrown by one of the connectors during gauge delta record with long") {
            whenever(myMetricConnector.gaugeDelta(any(), ArgumentMatchers.anyLong())).thenThrow(IllegalStateException("shouldn't throw it"))
            assertThat(catch { myMetricObject.gaugeDelta(1L) }).isNull()
            verify(myMetricConnector).gaugeDelta(eq(myMetricObject), eq(1L))
        }

        it("shouldn't fail if exception is thrown by one of the connectors during gauge delta record with double") {
            whenever(myMetricConnector.gaugeDelta(any(), ArgumentMatchers.anyDouble())).thenThrow(IllegalStateException("shouldn't throw it"))
            assertThat(catch { myMetricObject.gaugeDelta(1.0) }).isNull()
            verify(myMetricConnector).gaugeDelta(eq(myMetricObject), eq(1.0))
        }

        it("shouldn't fail if exception is thrown by one of the connectors during time record") {
            whenever(myMetricConnector.time(any(), any())).thenThrow(IllegalStateException("shouldn't throw it"))
            assertThat(catch { myMetricObject.time(1L) }).isNull()
            verify(myMetricConnector).time(eq(myMetricObject), eq(1L))
        }
    }
})