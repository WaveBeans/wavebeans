package io.wavebeans.metrics.collector

import assertk.all
import assertk.assertThat
import assertk.assertions.*
import assertk.fail
import io.grpc.ServerBuilder
import io.wavebeans.metrics.MetricObject
import io.wavebeans.metrics.MetricService
import io.wavebeans.metrics.eachIndexed
import io.wavebeans.tests.findFreePort
import org.spekframework.spek2.Spek
import org.spekframework.spek2.lifecycle.CachingMode.*
import org.spekframework.spek2.style.specification.describe
import java.lang.Thread.sleep

object MetricCollectorSpec : Spek({

    describe("Single mode") {

        describe("Counter") {
            val counter by memoized(TEST) { MetricObject.counter("component", "count", "") }

            val counterMetricCollector by memoized(TEST) { counter.collector(granularValueInMs = 0) }
            val counterMetricCollectorWithTag by memoized(TEST) { counter.withTags("tag" to "value").collector(granularValueInMs = 0) }

            beforeEachTest {
                MetricService.reset()
                MetricService.registerConnector(counterMetricCollector)
                MetricService.registerConnector(counterMetricCollectorWithTag)
            }

            it("should return 2 incremented values on collect") {
                counter.increment()
                counter.withTags("some-tag" to "some-value").increment()

                val counterValues = counterMetricCollector.collectValues(System.currentTimeMillis() + 100)
                assertThat(counterValues).all {
                    size().isEqualTo(2)
                    each { it.prop("value") { it.value }.isCloseTo(1.0, 1e-14) }
                }
                val counterValuesWithTag = counterMetricCollectorWithTag.collectValues(System.currentTimeMillis() + 100)
                assertThat(counterValuesWithTag).isEmpty()
            }

            it("should return 2 incremented values on collect for counter with tag") {
                counter.withTags("tag" to "value").increment()
                counter.withTags("tag" to "value", "some-tag" to "some-value").increment()

                val counterValuesWithTag = counterMetricCollectorWithTag.collectValues(System.currentTimeMillis() + 100)
                assertThat(counterValuesWithTag).all {
                    size().isEqualTo(2)
                    each { it.prop("value") { it.value }.isCloseTo(1.0, 1e-14) }
                }
                val counterValues = counterMetricCollector.collectValues(System.currentTimeMillis() + 100)
                assertThat(counterValues).all {
                    size().isEqualTo(2)
                    each { it.prop("value") { it.value }.isCloseTo(1.0, 1e-14) }
                }
            }

            it("should return incremented values on collect for each counter") {
                counter.increment()
                counter.withTags("tag" to "value").increment()

                val counterValues = counterMetricCollector.collectValues(System.currentTimeMillis() + 100)
                assertThat(counterValues).all {
                    size().isEqualTo(2)
                    each { it.prop("value") { it.value }.isCloseTo(1.0, 1e-14) }
                }
                val counterValuesWithTag = counterMetricCollectorWithTag.collectValues(System.currentTimeMillis() + 100)
                assertThat(counterValuesWithTag).all {
                    size().isEqualTo(1)
                    each { it.prop("value") { it.value }.isCloseTo(1.0, 1e-14) }
                }
            }

            it("should return 2 decremented values on collect") {
                counter.decrement()
                counter.withTags("some-tag" to "some-value").decrement()

                val counterValues = counterMetricCollector.collectValues(System.currentTimeMillis() + 100)
                assertThat(counterValues).all {
                    size().isEqualTo(2)
                    each { it.prop("value") { it.value }.isCloseTo(-1.0, 1e-14) }
                }
                val counterValuesWithTag = counterMetricCollectorWithTag.collectValues(System.currentTimeMillis() + 100)
                assertThat(counterValuesWithTag).isEmpty()
            }

            it("should return 2 decremented values on collect for counter with tag") {
                counter.withTags("tag" to "value").decrement()
                counter.withTags("tag" to "value", "some-tag" to "some-value").decrement()

                val counterValuesWithTag = counterMetricCollectorWithTag.collectValues(System.currentTimeMillis() + 100)
                assertThat(counterValuesWithTag).all {
                    size().isEqualTo(2)
                    each { it.prop("value") { it.value }.isCloseTo(-1.0, 1e-14) }
                }
                val counterValues = counterMetricCollector.collectValues(System.currentTimeMillis() + 100)
                assertThat(counterValues).all {
                    size().isEqualTo(2)
                    each { it.prop("value") { it.value }.isCloseTo(-1.0, 1e-14) }
                }
            }

            it("should return decremented values on collect for each counter") {
                counter.decrement()
                counter.withTags("tag" to "value").decrement()

                val counterValues = counterMetricCollector.collectValues(System.currentTimeMillis() + 100)
                assertThat(counterValues).all {
                    size().isEqualTo(2)
                    each { it.prop("value") { it.value }.isCloseTo(-1.0, 1e-14) }
                }
                val counterValuesWithTag = counterMetricCollectorWithTag.collectValues(System.currentTimeMillis() + 100)
                assertThat(counterValuesWithTag).all {
                    size().isEqualTo(1)
                    each { it.prop("value") { it.value }.isCloseTo(-1.0, 1e-14) }
                }
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
            val timeMetricCollectorWithTag by memoized(TEST) { time.withTags("tag" to "value").collector(granularValueInMs = 0) }

            beforeEachTest {
                MetricService.reset()
                MetricService.registerConnector(timeMetricCollector)
                MetricService.registerConnector(timeMetricCollectorWithTag)
            }

            it("should measure time twice") {
                time.time(100)
                time.withTags("some-tag" to "some-value").time(100)

                val values = timeMetricCollector.collectValues(System.currentTimeMillis() + 100)

                assertThat(values).all {
                    size().isEqualTo(2)
                    each { it.prop("value") { it.value }.isEqualTo(TimeAccumulator(1, 100)) }
                }

                val valuesWithTag = timeMetricCollectorWithTag.collectValues(System.currentTimeMillis() + 100)
                assertThat(valuesWithTag).isEmpty()
            }

            it("should measure time twice for counter with tags") {
                time.withTags("tag" to "value").time(100)
                time.withTags("tag" to "value", "some-tag" to "some-value").time(100)

                val valuesWithTag = timeMetricCollectorWithTag.collectValues(System.currentTimeMillis() + 100)

                assertThat(valuesWithTag).all {
                    size().isEqualTo(2)
                    each { it.prop("value") { it.value }.isEqualTo(TimeAccumulator(1, 100)) }
                }

                val values = timeMetricCollector.collectValues(System.currentTimeMillis() + 100)
                assertThat(values).all {
                    size().isEqualTo(2)
                    each { it.prop("value") { it.value }.isEqualTo(TimeAccumulator(1, 100)) }
                }
            }

            it("should measure time once for counter with tag and twice for tagless") {
                time.time(100)
                time.withTags("tag" to "value").time(100)

                val values = timeMetricCollector.collectValues(System.currentTimeMillis() + 100)

                assertThat(values).all {
                    size().isEqualTo(2)
                    each { it.prop("value") { it.value }.isEqualTo(TimeAccumulator(1, 100)) }
                }

                val valuesWithTag = timeMetricCollectorWithTag.collectValues(System.currentTimeMillis() + 100)
                assertThat(valuesWithTag).all {
                    size().isEqualTo(1)
                    each { it.prop("value") { it.value }.isEqualTo(TimeAccumulator(1, 100)) }
                }
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
            val gaugeMetricCollectorWithTag by memoized(TEST) { gauge.withTags("tag" to "value").collector(granularValueInMs = 0) }

            beforeEachTest {
                MetricService.reset()
                MetricService.registerConnector(gaugeMetricCollector)
                MetricService.registerConnector(gaugeMetricCollectorWithTag)
            }

            it("should set and add delta") {
                gauge.set(1.0)
                gauge.withTags("some-tag" to "some-value").increment(1.0)

                val values = gaugeMetricCollector.collectValues(System.currentTimeMillis() + 100)
                val valuesWithTag = gaugeMetricCollectorWithTag.collectValues(System.currentTimeMillis() + 100)

                assertThat(values).eachIndexed(2) { timedValue, idx ->
                    when (idx) {
                        0 -> timedValue.prop("value") { it.value }.isEqualTo(GaugeAccumulator(true, 1.0))
                        1 -> timedValue.prop("value") { it.value }.isEqualTo(GaugeAccumulator(false, 1.0))
                        else -> fail("$idx is not recognized")
                    }
                }
                assertThat(valuesWithTag).isEmpty()
            }

            it("should set and add delta for counter with tag") {
                gauge.withTags("tag" to "value").set(1.0)
                gauge.withTags("tag" to "value", "some-tag" to "some-value").increment(1.0)

                val values = gaugeMetricCollector.collectValues(System.currentTimeMillis() + 100)
                val valuesWithTag = gaugeMetricCollectorWithTag.collectValues(System.currentTimeMillis() + 100)

                assertThat(valuesWithTag).eachIndexed(2) { timedValue, idx ->
                    when (idx) {
                        0 -> timedValue.prop("value") { it.value }.isEqualTo(GaugeAccumulator(true, 1.0))
                        1 -> timedValue.prop("value") { it.value }.isEqualTo(GaugeAccumulator(false, 1.0))
                        else -> fail("$idx is not recognized")
                    }
                }
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
                gauge.withTags("tag" to "value").set(3.0)
                gauge.increment(2.1)

                val values = gaugeMetricCollector.collectValues(System.currentTimeMillis() + 100)
                val valuesWithTag = gaugeMetricCollectorWithTag.collectValues(System.currentTimeMillis() + 100)

                assertThat(values).eachIndexed(4) { timedValue, idx ->
                    when (idx) {
                        0 -> timedValue.prop("value") { it.value }.isEqualTo(GaugeAccumulator(true, 1.0))
                        1 -> timedValue.prop("value") { it.value }.isEqualTo(GaugeAccumulator(false, 1.0))
                        2 -> timedValue.prop("value") { it.value }.isEqualTo(GaugeAccumulator(true, 3.0))
                        3 -> timedValue.prop("value") { it.value }.isEqualTo(GaugeAccumulator(false, 2.1))
                        else -> fail("$idx is not recognized")
                    }
                }
                assertThat(valuesWithTag).eachIndexed(2) { timedValue, idx ->
                    when (idx) {
                        0 -> timedValue.prop("value") { it.value }.isEqualTo(GaugeAccumulator(false, 1.0))
                        1 -> timedValue.prop("value") { it.value }.isEqualTo(GaugeAccumulator(true, 3.0))
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

        describe("One downstream collector") {
            val port by memoized(EACH_GROUP) { findFreePort() }
            val counter by memoized(EACH_GROUP) { MetricObject.counter("component", "count", "") }
            val time by memoized(EACH_GROUP) { MetricObject.time("component", "time", "") }
            val gauge by memoized(EACH_GROUP) { MetricObject.gauge("component", "gauge", "") }

            val counterUpstream by memoized(EACH_GROUP) {
                counter.collector(
                        downstreamCollectors = listOf("localhost:$port"),
                        granularValueInMs = 0,
                        refreshIntervalMs = 0
                )
            }

            val counterUpstreamWithTag by memoized(EACH_GROUP) {
                counter.withTags("tag" to "value").collector(
                        downstreamCollectors = listOf("localhost:$port"),
                        granularValueInMs = 0,
                        refreshIntervalMs = 0
                )
            }

            val timeUpstream by memoized(EACH_GROUP) {
                time.collector(
                        downstreamCollectors = listOf("localhost:$port"),
                        granularValueInMs = 0,
                        refreshIntervalMs = 0
                )
            }

            val timeUpstreamWithTag by memoized(EACH_GROUP) {
                time.withTags("tag" to "value").collector(
                        downstreamCollectors = listOf("localhost:$port"),
                        granularValueInMs = 0,
                        refreshIntervalMs = 0
                )
            }

            val gaugeUpstream by memoized(EACH_GROUP) {
                gauge.collector(
                        downstreamCollectors = listOf("localhost:$port"),
                        granularValueInMs = 0,
                        refreshIntervalMs = 0
                )
            }

            val gaugeUpstreamWithTag by memoized(EACH_GROUP) {
                gauge.withTags("tag" to "value").collector(
                        downstreamCollectors = listOf("localhost:$port"),
                        granularValueInMs = 0,
                        refreshIntervalMs = 0
                )
            }

            val server by memoized(EACH_GROUP) {
                ServerBuilder.forPort(port)
                        .addService(MetricGrpcService.instance())
                        .build()
            }

            beforeGroup {
                server.start()
                assertThat(counterUpstream.attachCollector()).isTrue()
                assertThat(counterUpstreamWithTag.attachCollector()).isTrue()
                assertThat(timeUpstream.attachCollector()).isTrue()
                assertThat(timeUpstreamWithTag.attachCollector()).isTrue()
                assertThat(gaugeUpstream.attachCollector()).isTrue()
                assertThat(gaugeUpstreamWithTag.attachCollector()).isTrue()
            }

            afterGroup {
                server.shutdownNow()
                counterUpstream.close()
                counterUpstreamWithTag.close()
                timeUpstream.close()
                timeUpstreamWithTag.close()
                gaugeUpstream.close()
                gaugeUpstreamWithTag.close()
            }

            it("should attach on first iteration") {
                counterUpstream.collectFromDownstream()
                assertThat(counterUpstream.awaitAttached()).isTrue()
                timeUpstream.collectFromDownstream()
                assertThat(timeUpstream.awaitAttached()).isTrue()
                gaugeUpstream.collectFromDownstream()
                assertThat(gaugeUpstream.awaitAttached()).isTrue()
            }

            it("should get counter values from downstream collector") {
                counter.increment(1.0)
                counter.increment(2.0)
                counterUpstream.increment(counter, 3.0)

                val afterEventMoment = System.currentTimeMillis() + 100

                counterUpstream.mergeWithDownstreamCollectors(afterEventMoment)

                assertThat(counterUpstream.collectValues(afterEventMoment))
                        .prop("values.toSet") { it.map { it.value }.toSet() }
                        .isEqualTo(setOf(1.0, 2.0, 3.0))
            }

            it("should get counter values from downstream collector respecting tags") {
                counter.increment(1.0)
                counter.withTags("tag" to "value").increment(2.0)
                counterUpstreamWithTag.increment(counter.withTags("anotherTag" to "anotherValue"), 3.0)

                val afterEventMoment = System.currentTimeMillis() + 100

                counterUpstreamWithTag.mergeWithDownstreamCollectors(afterEventMoment)

                assertThat(counterUpstreamWithTag.collectValues(afterEventMoment))
                        .prop("values.toSet") { it.map { it.value }.toSet() }
                        .isEqualTo(setOf(2.0))
            }

            it("should get time values from downstream collector") {
                time.time(100)
                time.time(200)
                timeUpstream.time(time, 300)

                val afterEventMoment = System.currentTimeMillis() + 100

                timeUpstream.mergeWithDownstreamCollectors(afterEventMoment)

                assertThat(timeUpstream.collectValues(afterEventMoment))
                        .prop("valuesAsSet") { it.map { it.value }.toSet() }
                        .isEqualTo(setOf(TimeAccumulator(1, 100), TimeAccumulator(1, 200), TimeAccumulator(1, 300)))
            }

            it("should get time values from downstream collector respecting tags") {
                time.time(100)
                time.withTags("tag" to "value").time(200)
                timeUpstreamWithTag.time(time.withTags("anotherTag" to "anotherValue"), 300)

                val afterEventMoment = System.currentTimeMillis() + 100

                timeUpstreamWithTag.mergeWithDownstreamCollectors(afterEventMoment)

                assertThat(timeUpstreamWithTag.collectValues(afterEventMoment))
                        .prop("valuesAsSet") { it.map { it.value }.toSet() }
                        .isEqualTo(setOf(TimeAccumulator(1, 200)))
            }

            it("should populate gauge values from downstream collector") {
                gauge.set(100.0)
                gauge.increment(200.0)
                gaugeUpstream.gauge(gauge, 300.0)

                val afterEventMoment = System.currentTimeMillis() + 100

                gaugeUpstream.mergeWithDownstreamCollectors(afterEventMoment)

                assertThat(gaugeUpstream.collectValues(afterEventMoment))
                        .prop("valuesAsSet") { it.map { it.value }.toSet() }
                        .isEqualTo(setOf(GaugeAccumulator(true, 100.0), GaugeAccumulator(false, 200.0), GaugeAccumulator(true, 300.0)))
            }

            it("should populate gauge values from downstream collector respecting tags") {
                gauge.set(100.0)
                gauge.withTags("tag" to "value").increment(200.0)
                gaugeUpstreamWithTag.gauge(gauge.withTags("anotherTag" to "anotherValue"), 300.0)

                val afterEventMoment = System.currentTimeMillis() + 100

                gaugeUpstreamWithTag.mergeWithDownstreamCollectors(afterEventMoment)

                assertThat(gaugeUpstreamWithTag.collectValues(afterEventMoment))
                        .prop("valuesAsSet") { it.map { it.value }.toSet() }
                        .isEqualTo(setOf(GaugeAccumulator(false, 200.0)))
            }
        }

        describe("Collecting automatically") {
            val port by memoized(EACH_GROUP) { findFreePort() }
            val counter by memoized(EACH_GROUP) { MetricObject.counter("component1", "name1", "") }

            val remoteMetricCollector by memoized(EACH_GROUP) {
                counter.collector(
                        downstreamCollectors = listOf("localhost:$port"),
                        granularValueInMs = 0,
                        refreshIntervalMs = 1
                )
            }

            val server by memoized(EACH_GROUP) {
                ServerBuilder.forPort(port)
                        .addService(MetricGrpcService.instance())
                        .build()
            }

            beforeGroup {
                server.start()
            }

            afterGroup {
                remoteMetricCollector.close()
                server.shutdownNow()
            }

            it("should attach on first iteration") {
                assertThat(remoteMetricCollector.awaitAttached()).isTrue()
            }

            it("should get values from downstream collector") {
                counter.increment(1.0)
                counter.increment(2.0)
                remoteMetricCollector.increment(counter, 3.0)

                val afterEventMoment = System.currentTimeMillis() + 100

                sleep(500) // wait for upstream metric collector to do a few syncs

                assertThat(remoteMetricCollector.collectValues(afterEventMoment))
                        .prop("values.toSet") { it.map { it.value }.toSet() }
                        .isEqualTo(setOf(1.0, 2.0, 3.0))
            }
        }

        describe("Two downstream collectors") {
            val port1 by memoized(EACH_GROUP) { findFreePort() }
            val port2 by memoized(EACH_GROUP) { findFreePort() }

            val counter by memoized(EACH_GROUP) { MetricObject.counter("component1_TwoDownstreamCollectors", "name1_TwoDownstreamCollectors", "") }

            val remoteMetricCollector by memoized(EACH_GROUP) {
                counter.collector(
                        downstreamCollectors = listOf("localhost:$port1", "localhost:$port2"),
                        granularValueInMs = 0,
                        refreshIntervalMs = 1
                )
            }

            val server1 by memoized(EACH_GROUP) {
                ServerBuilder.forPort(port1)
                        .addService(MetricGrpcService.instance())
                        .build()
            }

            val server2 by memoized(EACH_GROUP) {
                ServerBuilder.forPort(port2)
                        .addService(MetricGrpcService.instance())
                        .build()
            }


            beforeGroup {
                server1.start()
                server2.start()
                // attach to downstream collectors
                assertThat(remoteMetricCollector.attachCollector()).isTrue()
            }

            afterGroup {
                remoteMetricCollector.close()
                server1.shutdownNow()
                server2.shutdownNow()
            }

            it("should get values from downstream collector") {
                assertThat(remoteMetricCollector.isAttached()).isTrue()
                counter.increment(1.0)
                counter.increment(2.0)
                counter.increment(3.0)
                counter.increment(4.0)
                remoteMetricCollector.increment(counter, 5.0)

                remoteMetricCollector.mergeWithDownstreamCollectors(Long.MAX_VALUE)

                assertThat(remoteMetricCollector.collectValues(Long.MAX_VALUE))
                        .prop("values.toList().sorted()") { it.map { it.value }.sorted() }
                        .isEqualTo(listOf(1.0, 1.0, 2.0, 2.0, 3.0, 3.0, 4.0, 4.0, 5.0))
            }
        }
    }
})