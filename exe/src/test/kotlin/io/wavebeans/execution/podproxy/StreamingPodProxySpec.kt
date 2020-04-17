package io.wavebeans.execution.podproxy

import assertk.all
import assertk.assertThat
import assertk.assertions.*
import assertk.catch
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.wavebeans.execution.*
import io.wavebeans.execution.config.ExecutionConfig
import io.wavebeans.execution.medium.PlainMediumBuilder
import io.wavebeans.execution.medium.PlainPodCallResultBuilder
import io.wavebeans.execution.medium.PodCallResult
import io.wavebeans.execution.pod.Pod
import io.wavebeans.execution.pod.PodKey
import io.wavebeans.lib.Sample
import io.wavebeans.lib.asInt
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

class PodProxyTester(
        val pointedTo: Pod,
        val timeToReadAtOnce: Int = 1
) {
    val podDiscovery = mock<PodDiscovery>()
            .also { whenever(it.bushFor(any())).thenReturn(newBushKey()) }

    var iteratorStartCounter: Int = 0
        private set

    var iteratorNextCounter: Int = 0
        private set

    val bushCaller = object : BushCaller {

        override fun call(request: String): Future<PodCallResult> {
            val call = Call.parseRequest(request)
            when (call.method) {
                "iteratorStart" -> {
                    iteratorStartCounter++
                }
                "iteratorNext" -> {
                    iteratorNextCounter++
                }
                else -> throw UnsupportedOperationException()
            }
            val f = CompletableFuture<PodCallResult>()
            f.complete(pointedTo.call(call))
            return f
        }
    }

    val bushCallerRepository = object : BushCallerRepository(podDiscovery) {
        override fun create(bushKey: BushKey, podKey: PodKey): BushCaller = bushCaller
    }

    val podProxy = object : StreamingPodProxy(
            pointedTo = pointedTo.podKey,
            forPartition = 0,
            bushCallerRepository = bushCallerRepository,
            podDiscovery = podDiscovery,
            prefetchBucketAmount = timeToReadAtOnce,
            partitionSize = 1
    ) {}
}


object StreamingPodProxySpec : Spek({

    ExecutionConfig.podCallResultBuilder(PlainPodCallResultBuilder())
    ExecutionConfig.mediumBuilder(PlainMediumBuilder())

    describe("Pod Proxy count amount of calls to Pod") {
        val podProxyTester = PodProxyTester(
                pointedTo = newTestStreamingPod((1..10).toList()),
                timeToReadAtOnce = 5
        )

        val seq = podProxyTester.podProxy.asSequence(20.0f)
        it("should create a sequence") { assertThat(seq).isNotNull() }
        it("should call iteratorStart once") { assertThat(podProxyTester.iteratorStartCounter).isEqualTo(1) }
        val res = seq.take(10).map { (it as Sample).asInt() }.toList()
        it("should read all samples") { assertThat(res).isEqualTo((1..10).toList()) }
        it("should call iteratorNext 2 times") {
            assertThat(podProxyTester.iteratorNextCounter).isEqualTo(2)
        }
    }

    describe("Iterator testing") {
        describe("data fits in one buffer") {
            val seq = PodProxyTester(
                    pointedTo = newTestStreamingPod((1..2).toList()),
                    timeToReadAtOnce = 2
            ).podProxy.asSequence(2.0f)
            val iterator = seq.iterator()

            it("should have value on 1st iteration") {
                assertThat(iterator).all {
                    prop("hasNext") { it.hasNext() }.isEqualTo(true)
                    prop("next") { (it.next() as Sample).asInt() }.isEqualTo(1)
                }
            }
            it("should have value on 2nd iteration") {
                assertThat(iterator).all {
                    prop("hasNext") { it.hasNext() }.isEqualTo(true)
                    prop("next") { (it.next() as Sample).asInt() }.isEqualTo(2)
                }
            }
            it("should not have value on 3rd iteration") {
                assertThat(iterator)
                        .prop("hasNext") { it.hasNext() }.isEqualTo(false)
                assertThat(catch { iterator.next() })
                        .isNotNull()
                        .isInstanceOf(NoSuchElementException::class)
                        .message()
                        .isNotNull()
                        .isNotEmpty()
            }
        }

        describe("data doesn't fit in one buffer") {
            val seq = PodProxyTester(
                    pointedTo = newTestStreamingPod((1..2).toList()),
                    timeToReadAtOnce = 2
            ).podProxy.asSequence(1.0f)
            val iterator = seq.iterator()

            it("should have value on 1st iteration") {
                assertThat(iterator).all {
                    prop("hasNext") { it.hasNext() }.isEqualTo(true)
                    prop("next") { (it.next() as Sample).asInt() }.isEqualTo(1)
                }
            }
            it("should have value on 2nd iteration") {
                assertThat(iterator).all {
                    prop("hasNext") { it.hasNext() }.isEqualTo(true)
                    prop("next") { (it.next() as Sample).asInt() }.isEqualTo(2)
                }
            }
            it("should not have value on 3rd iteration") {
                assertThat(iterator)
                        .prop("hasNext") { it.hasNext() }.isEqualTo(false)
                assertThat(catch { iterator.next() })
                        .isNotNull()
                        .isInstanceOf(NoSuchElementException::class)
                        .message()
                        .isNotNull()
                        .isNotEmpty()
            }
        }


    }
})