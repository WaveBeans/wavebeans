package io.wavebeans.metrics.collector

import assertk.assertThat
import assertk.assertions.each
import assertk.assertions.isCloseTo
import assertk.assertions.isEqualTo
import assertk.assertions.prop
import assertk.fail
import io.grpc.ServerBuilder
import io.wavebeans.metrics.MetricObject
import io.wavebeans.metrics.MetricService
import io.wavebeans.metrics.eachIndexed
import org.spekframework.spek2.Spek
import org.spekframework.spek2.lifecycle.CachingMode.SCOPE
import org.spekframework.spek2.lifecycle.CachingMode.TEST
import org.spekframework.spek2.style.specification.describe
import java.lang.Thread.sleep

object MetricCollectorSpec : Spek({

    describe("Single mode") {

        describe("Counter") {
            val counter by memoized(TEST) { MetricObject.counter("component", "count", "") }

            val counterMetricCollector by memoized(TEST) { counter.collector(granularValueInMs = 0) }

            beforeEachTest {
                MetricService.reset()
                MetricService.registerConnector(counterMetricCollector)
            }

            it("should return 2 incremented values on collect") {
                counter.increment()
                counter.withTags("tag" to "value").increment()

                val counterValues = counterMetricCollector.collectValues(System.currentTimeMillis() + 100)
                assertThat(counterValues).each { it.prop("value") { it.value }.isCloseTo(1.0, 1e-14) }
            }

            it("should return 2 decremented values on collect") {
                counter.decrement()
                counter.withTags("tag" to "value").decrement()

                val values = counterMetricCollector.collectValues(System.currentTimeMillis() + 100)

                assertThat(values).each { it.prop("value") { it.value }.isCloseTo(-1.0, 1e-14) }
            }

            it("should merge with another") {
                counter.increment()

                counterMetricCollector.merge(sequenceOf(
                        2.0 at 2L,
                        3.0 at 3L,
                        5.0 at System.currentTimeMillis() + 5000
                ))

                val values = counterMetricCollector.collectValues(Long.MAX_VALUE)

                assertThat(values).eachIndexed(4) { timedValue, idx ->
                    when (idx) {
                        0 -> timedValue.isEqualTo(2.0 at 2L)
                        1 -> timedValue.isEqualTo(3.0 at 3L)
                        2 -> timedValue.prop("value") { it.value }.isCloseTo(1.0, 1e-14)
                        3 -> timedValue.prop("value") { it.value }.isCloseTo(5.0, 1e-14)
                        else -> fail("$idx is not recognized")
                    }
                }
            }
        }

        describe("Time") {
            val time by memoized(TEST) { MetricObject.time("component", "time", "") }

            val timeMetricCollector by memoized(TEST) { time.collector(granularValueInMs = 0) }

            beforeEachTest {
                MetricService.reset()
                MetricService.registerConnector(timeMetricCollector)
            }

            it("should measure time twice") {
                time.time(100)
                time.withTags("tag" to "value").time(100)

                val values = timeMetricCollector.collectValues(System.currentTimeMillis() + 100)

                assertThat(values).each { it.prop("value") { it.value }.isEqualTo(TimeAccumulator(1, 100)) }
            }

            it("should merge with another") {
                time.time(100)

                timeMetricCollector.merge(sequenceOf(
                        TimeAccumulator(1, 200) at 2L,
                        TimeAccumulator(1, 300) at 3L,
                        TimeAccumulator(1, 500) at System.currentTimeMillis() + 5000
                ))

                val values = timeMetricCollector.collectValues(Long.MAX_VALUE)

                assertThat(values).eachIndexed(4) { timedValue, idx ->
                    when (idx) {
                        0 -> timedValue.isEqualTo(TimeAccumulator(1, 200) at 2L)
                        1 -> timedValue.isEqualTo(TimeAccumulator(1, 300) at 3L)
                        2 -> timedValue.prop("value.time") { it.value.time }.isEqualTo(100L)
                        3 -> timedValue.prop("value.time") { it.value.time }.isEqualTo(500L)
                        else -> fail("$idx is not recognized")
                    }
                }
            }
        }

        describe("Gauge") {
            val gauge by memoized(TEST) { MetricObject.gauge("component", "gauge", "") }

            val gaugeMetricCollector by memoized(TEST) { gauge.collector(granularValueInMs = 0) }

            beforeEachTest {
                MetricService.reset()
                MetricService.registerConnector(gaugeMetricCollector)
            }

            it("should set and add delta") {
                gauge.set(1.0)
                gauge.withTags("tag" to "value").increment(1.0)

                val values = gaugeMetricCollector.collectValues(System.currentTimeMillis() + 100)

                assertThat(values).eachIndexed(2) { timedValue, idx ->
                    when (idx) {
                        0 -> timedValue.prop("value") { it.value }.isEqualTo(GaugeAccumulator(true, 1.0))
                        1 -> timedValue.prop("value") { it.value }.isEqualTo(GaugeAccumulator(false, 1.0))
                        else -> fail("$idx is not recognized")
                    }
                }
            }

            it("should set over the collected value") {
                gauge.set(1.0)
                gauge.withTags("tag" to "value").increment(1.0)
                gauge.set(2.1)

                val values = gaugeMetricCollector.collectValues(System.currentTimeMillis() + 100)

                assertThat(values).eachIndexed(3) { timedValue, idx ->
                    when (idx) {
                        0 -> timedValue.prop("value") { it.value }.isEqualTo(GaugeAccumulator(true, 1.0))
                        1 -> timedValue.prop("value") { it.value }.isEqualTo(GaugeAccumulator(false, 1.0))
                        2 -> timedValue.prop("value") { it.value }.isEqualTo(GaugeAccumulator(true, 2.1))
                        else -> fail("$idx is not recognized")
                    }
                }
            }

            it("should merge with another") {
                gauge.set(1.0)

                gaugeMetricCollector.merge(sequenceOf(
                        GaugeAccumulator(false, 2.0) at 2L,
                        GaugeAccumulator(false, 3.0) at 3L,
                        GaugeAccumulator(false, 5.0) at System.currentTimeMillis() + 5000
                ))

                val values = gaugeMetricCollector.collectValues(Long.MAX_VALUE)

                assertThat(values).eachIndexed(4) { timedValue, idx ->
                    when (idx) {
                        0 -> timedValue.prop("value") { it.value }.isEqualTo(GaugeAccumulator(false, 2.0))
                        1 -> timedValue.prop("value") { it.value }.isEqualTo(GaugeAccumulator(false, 3.0))
                        2 -> timedValue.prop("value") { it.value }.isEqualTo(GaugeAccumulator(true, 1.0))
                        3 -> timedValue.prop("value") { it.value }.isEqualTo(GaugeAccumulator(false, 5.0))
                        else -> fail("$idx is not recognized")
                    }
                }
            }
        }
    }

    describe("Distributed mode") {

        describe("One downstream collectors") {
            val port = 40000

            val counter by memoized(SCOPE) { MetricObject.counter("component", "count", "") }
            val time by memoized(SCOPE) { MetricObject.time("component", "time", "") }
            val gauge by memoized(SCOPE) { MetricObject.gauge("component", "gauge", "") }

            val counterDownstream by memoized(SCOPE) { counter.collector(granularValueInMs = 0) }
            val timeDownstream by memoized(SCOPE) { time.collector(granularValueInMs = 0) }
            val gaugeDownstream by memoized(SCOPE) { gauge.collector(granularValueInMs = 0) }

            val counterUpstream by memoized(SCOPE) {
                counter.collector(
                        downstreamCollectors = listOf("localhost:$port"),
                        granularValueInMs = 0,
                        refreshIntervalMs = Long.MAX_VALUE
                )
            }

            val timeUpstream by memoized(SCOPE) {
                time.collector(
                        downstreamCollectors = listOf("localhost:$port"),
                        granularValueInMs = 0,
                        refreshIntervalMs = Long.MAX_VALUE
                )
            }

            val gaugeUpstream by memoized(SCOPE) {
                gauge.collector(
                        downstreamCollectors = listOf("localhost:$port"),
                        granularValueInMs = 0,
                        refreshIntervalMs = Long.MAX_VALUE
                )
            }

            val server by memoized(SCOPE) {
                ServerBuilder.forPort(port)
                        .addService(
                                MetricGrpcService.instance()
                                        .attachCollector(counterDownstream)
                                        .attachCollector(timeDownstream)
                                        .attachCollector(gaugeDownstream)
                        )
                        .build()

            }

            beforeGroup {
                server.start()
            }

            afterGroup {
                server.shutdownNow()
                counterDownstream.close()
                timeDownstream.close()
                gaugeDownstream.close()
                counterUpstream.close()
                timeUpstream.close()
                gaugeUpstream.close()
            }

            it("should get counter values from downstream collector") {
                counterDownstream.increment(counter, 1.0)
                counterDownstream.increment(counter.withTags("tag" to "value"), 2.0)
                counterUpstream.increment(counter.withTags("anotherTag" to "anotherValue"), 3.0)

                val afterEventMoment = System.currentTimeMillis() + 1

                counterUpstream.mergeWithDownstreamCollectors(afterEventMoment)

                assertThat(counterUpstream.collectValues(afterEventMoment))
                        .prop("values.toSet") { it.map { it.value }.toSet() }
                        .isEqualTo(setOf(1.0, 2.0, 3.0))
            }

            it("should get time values from downstream collector") {
                timeDownstream.time(time, 100)
                timeDownstream.time(time.withTags("tag" to "value"), 200)
                timeUpstream.time(time.withTags("anotherTag" to "anotherValue"), 300)

                val afterEventMoment = System.currentTimeMillis() + 1

                timeUpstream.mergeWithDownstreamCollectors(afterEventMoment)

                assertThat(timeUpstream.collectValues(afterEventMoment))
                        .prop("valuesAsSet") { it.map { it.value }.toSet() }
                        .isEqualTo(setOf(TimeAccumulator(1, 100), TimeAccumulator(1, 200), TimeAccumulator(1, 300)))
            }

            it("should populate gauge values from downstream collector") {
                gaugeDownstream.gauge(gauge, 100.0)
                gaugeDownstream.gaugeDelta(gauge.withTags("tag" to "value"), 200.0)
                gaugeUpstream.gauge(gauge.withTags("anotherTag" to "anotherValue"), 300.0)

                val afterEventMoment = System.currentTimeMillis() + 1

                gaugeUpstream.mergeWithDownstreamCollectors(afterEventMoment)

                assertThat(gaugeUpstream.collectValues(afterEventMoment))
                        .prop("valuesAsSet") { it.map { it.value }.toSet() }
                        .isEqualTo(setOf(GaugeAccumulator(true, 100.0), GaugeAccumulator(false, 200.0), GaugeAccumulator(true, 300.0)))
            }
        }

        describe("Collecting automatically") {
            val port = 40000

            val counter by memoized(SCOPE) { MetricObject.counter("component1", "name1", "") }

            val downstreamMetricCollector by memoized(SCOPE) { counter.collector(granularValueInMs = 0) }

            val upstreamMetricCollector by memoized(SCOPE) {
                counter.collector(
                        downstreamCollectors = listOf("localhost:$port"),
                        granularValueInMs = 0,
                        refreshIntervalMs = 1
                )
            }

            val server by memoized(SCOPE) {
                ServerBuilder.forPort(port)
                        .addService(MetricGrpcService.instance().attachCollector(downstreamMetricCollector))
                        .build()
            }

            beforeGroup { server.start() }

            afterGroup {
                server.shutdownNow()
                downstreamMetricCollector.close()
                upstreamMetricCollector.close()
            }

            it("should get values from downstream collector") {
                downstreamMetricCollector.increment(counter, 1.0)
                downstreamMetricCollector.increment(counter.withTags("tag" to "value"), 2.0)
                upstreamMetricCollector.increment(counter.withTags("anotherTag" to "anotherValue"), 3.0)

                val afterEventMoment = System.currentTimeMillis() + 1

                sleep(500) // wait for upstream metric collector to do a few syncs

                assertThat(upstreamMetricCollector.collectValues(afterEventMoment))
                        .prop("values.toSet") { it.map { it.value }.toSet() }
                        .isEqualTo(setOf(1.0, 2.0, 3.0))
            }
        }

        describe("Two downstream collectors") {
            val port1 = 40000
            val port2 = 40001

            val counter by memoized(SCOPE) { MetricObject.counter("component1", "name1", "") }

            val downstreamMetricCollector1 by memoized(SCOPE) { counter.collector(granularValueInMs = 0) }
            val downstreamMetricCollector2 by memoized(SCOPE) { counter.collector(granularValueInMs = 0) }

            val upstreamMetricCollector by memoized(SCOPE) {
                counter.collector(
                        downstreamCollectors = listOf("localhost:$port1", "localhost:$port2"),
                        granularValueInMs = 0,
                        refreshIntervalMs = 1
                )
            }

            val server1 by memoized(SCOPE) {
                ServerBuilder.forPort(port1)
                        .addService(MetricGrpcService.instance().attachCollector(downstreamMetricCollector1))
                        .build()
            }

            val server2 by memoized(SCOPE) {
                ServerBuilder.forPort(port2)
                        .addService(MetricGrpcService.instance().attachCollector(downstreamMetricCollector2))
                        .build()
            }


            beforeGroup {
                server1.start()
                server2.start()
            }

            afterGroup {
                server1.shutdownNow()
                server2.shutdownNow()
                downstreamMetricCollector1.close()
                downstreamMetricCollector2.close()
                upstreamMetricCollector.close()
            }

            it("should get values from downstream collector") {
                downstreamMetricCollector1.increment(counter, 1.0)
                downstreamMetricCollector2.increment(counter, 2.0)
                downstreamMetricCollector1.increment(counter.withTags("tag" to "value"), 3.0)
                downstreamMetricCollector2.increment(counter.withTags("tag" to "value"), 4.0)
                upstreamMetricCollector.increment(counter.withTags("anotherTag" to "anotherValue"), 5.0)

                val afterEventMoment = System.currentTimeMillis() + 1

                upstreamMetricCollector.mergeWithDownstreamCollectors(afterEventMoment)

                assertThat(upstreamMetricCollector.collectValues(afterEventMoment))
                        .prop("values.toSet") { it.map { it.value }.toSet() }
                        .isEqualTo(setOf(1.0, 2.0, 3.0, 4.0, 5.0))
            }
        }
    }
})