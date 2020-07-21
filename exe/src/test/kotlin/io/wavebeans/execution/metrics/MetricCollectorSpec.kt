package io.wavebeans.execution.metrics

import assertk.assertThat
import assertk.assertions.*
import assertk.fail
import io.grpc.ServerBuilder
import io.wavebeans.execution.eachIndexed
import io.wavebeans.lib.s
import org.spekframework.spek2.Spek
import org.spekframework.spek2.lifecycle.CachingMode.SCOPE
import org.spekframework.spek2.style.specification.describe
import java.lang.Thread.sleep

object MetricCollectorSpec : Spek({

    describe("Single mode") {

        val metricCollector by memoized(SCOPE) { MetricCollector(granularValueInMs = 0) }

        beforeGroup {
            MetricService.reset()
            MetricService.registerConnector(metricCollector)
        }

        it("should return 2 incremented values on collect") {
            val mo = MetricObject("component1", "name1")
            mo.increment()
            mo.withTags("tag" to "value").increment()

            val values = metricCollector
                    .collectValues(System.currentTimeMillis() + 100)
                    .first { it.first.isLike(mo) }

            assertThat(values.second).each { it.prop("second") { it.second }.isCloseTo(1.0, 1e-14) }
        }

        it("should return 2 decremented values on collect") {
            val mo = MetricObject("component2", "name2")
            mo.decrement()
            mo.withTags("tag" to "value").decrement()

            val values = metricCollector
                    .collectValues(System.currentTimeMillis() + 100)
                    .first { it.first.isLike(mo) }

            assertThat(values.second).each { it.prop("second") { it.second }.isCloseTo(-1.0, 1e-14) }
        }

        it("should merge") {
            val mo = MetricObject("component3", "name3")
            mo.increment()

            metricCollector.merge(sequenceOf(
                    MetricObject("component3", "name3") to listOf(
                            2L to 2.0,
                            3L to 3.0,
                            System.currentTimeMillis() + 5000 to 5.0
                    )
            ))

            val values = metricCollector
                    .collectValues(Long.MAX_VALUE)
                    .first { it.first.isLike(mo) }

            assertThat(values.second).eachIndexed(4) { pair, idx ->
                when (idx) {
                    0 -> pair.isEqualTo(2L to 2.0)
                    1 -> pair.isEqualTo(3L to 3.0)
                    2 -> pair.prop("second") { it.second }.isCloseTo(1.0, 1e-14)
                    3 -> pair.prop("second") { it.second }.isCloseTo(5.0, 1e-14)
                    else -> fail("$idx is not recognized")
                }
            }
        }
    }

    describe("Distributed mode") {

        describe("One downstream collectors") {
            val port = 40000

            val downstreamMetricCollector by memoized(SCOPE) { MetricCollector(granularValueInMs = 0) }

            val upstreamMetricCollector by memoized(SCOPE) { MetricCollector(listOf("localhost:$port"), granularValueInMs = 0, refreshIntervalMs = Long.MAX_VALUE) }

            val server by memoized(SCOPE) {
                ServerBuilder.forPort(port)
                        .addService(MetricGrpcService.instance(downstreamMetricCollector))
                        .build()
            }

            beforeGroup { server.start() }

            afterGroup {
                server.shutdownNow()
                downstreamMetricCollector.close()
                upstreamMetricCollector.close()
            }

            it("should get values from downstream collector") {
                val mo = MetricObject("1", "2")
                downstreamMetricCollector.increment(mo, 1)
                downstreamMetricCollector.increment(mo.withTags("tag" to "value"), 2)
                upstreamMetricCollector.increment(mo.withTags("anotherTag" to "anotherValue"), 3)

                val afterEventMoment = System.currentTimeMillis() + 1

                upstreamMetricCollector.mergeWithDownstreamCollectors(afterEventMoment)

                assertThat(upstreamMetricCollector.collectValues(afterEventMoment))
                        .prop("MetricObject[1,2].values") { it.first { it.first.isLike(MetricObject("1", "2")) }.second }
                        .prop("values.toSet") { it.map { it.second }.toSet() }
                        .isEqualTo(setOf(1.0, 2.0, 3.0))
            }
        }

        describe("Collecting automatically") {
            val port = 40000

            val downstreamMetricCollector by memoized(SCOPE) { MetricCollector(granularValueInMs = 0) }

            val upstreamMetricCollector by memoized(SCOPE) { MetricCollector(listOf("localhost:$port"), granularValueInMs = 0, refreshIntervalMs = 1) }

            val server by memoized(SCOPE) {
                ServerBuilder.forPort(port)
                        .addService(MetricGrpcService.instance(downstreamMetricCollector))
                        .build()
            }

            beforeGroup { server.start() }

            afterGroup {
                server.shutdownNow()
                downstreamMetricCollector.close()
                upstreamMetricCollector.close()
            }

            it("should get values from downstream collector") {
                val mo = MetricObject("1", "2")
                downstreamMetricCollector.increment(mo, 1)
                downstreamMetricCollector.increment(mo.withTags("tag" to "value"), 2)
                upstreamMetricCollector.increment(mo.withTags("anotherTag" to "anotherValue"), 3)

                val afterEventMoment = System.currentTimeMillis() + 1

                sleep(100) // wait for upstream metric collector to do a few syncs

                assertThat(upstreamMetricCollector.collectValues(afterEventMoment))
                        .prop("MetricObject[1,2].values") { it.first { it.first.isLike(MetricObject("1", "2")) }.second }
                        .prop("values.toSet") { it.map { it.second }.toSet() }
                        .isEqualTo(setOf(1.0, 2.0, 3.0))
            }
        }

        describe("Two downstream collectors") {
            val port1 = 40000
            val port2 = 40001

            val downstreamMetricCollector1 by memoized(SCOPE) { MetricCollector(granularValueInMs = 0) }
            val downstreamMetricCollector2 by memoized(SCOPE) { MetricCollector(granularValueInMs = 0) }

            val upstreamMetricCollector by memoized(SCOPE) { MetricCollector(listOf("localhost:$port1", "localhost:$port2"), granularValueInMs = 0, refreshIntervalMs = Long.MAX_VALUE) }

            val server1 by memoized(SCOPE) {
                ServerBuilder.forPort(port1)
                        .addService(MetricGrpcService.instance(downstreamMetricCollector1))
                        .build()
            }

            val server2 by memoized(SCOPE) {
                ServerBuilder.forPort(port2)
                        .addService(MetricGrpcService.instance(downstreamMetricCollector2))
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
                val mo = MetricObject("1", "2")
                downstreamMetricCollector1.increment(mo, 1)
                downstreamMetricCollector2.increment(mo, 2)
                downstreamMetricCollector1.increment(mo.withTags("tag" to "value"), 3)
                downstreamMetricCollector2.increment(mo.withTags("tag" to "value"), 4)
                upstreamMetricCollector.increment(mo.withTags("anotherTag" to "anotherValue"), 5)

                val afterEventMoment = System.currentTimeMillis() + 1

                upstreamMetricCollector.mergeWithDownstreamCollectors(afterEventMoment)

                assertThat(upstreamMetricCollector.collectValues(afterEventMoment))
                        .prop("MetricObject[1,2].values") { it.first { it.first.isLike(MetricObject("1", "2")) }.second }
                        .prop("values.toSet") { it.map { it.second }.toSet() }
                        .isEqualTo(setOf(1.0, 2.0, 3.0, 4.0, 5.0))
            }
        }
    }
})