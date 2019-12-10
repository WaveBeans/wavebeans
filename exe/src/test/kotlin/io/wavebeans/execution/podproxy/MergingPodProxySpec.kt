package io.wavebeans.execution.podproxy

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import com.nhaarman.mockitokotlin2.mock
import io.wavebeans.execution.*
import io.wavebeans.execution.medium.PodCallResult
import io.wavebeans.execution.medium.SampleArray
import io.wavebeans.execution.medium.nullableSampleArrayList
import io.wavebeans.execution.pod.Pod
import io.wavebeans.execution.pod.PodKey
import io.wavebeans.lib.Sample
import io.wavebeans.lib.asInt
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

@ExperimentalStdlibApi
class MergingPodProxyTester(
        val readsFrom: List<Pod>,
        val timeToReadAtOnce: Int,
        val partitionSize: Int
) {
    val podDiscovery: PodDiscovery = mock()

    var iteratorStartCounter: Int = 0
        private set

    var iteratorNextCounter: Int = 0
        private set

    fun bushCaller(podKey: PodKey) = object : BushCaller {

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
            f.complete(readsFrom.first { it.podKey == podKey }.call(call))
            return f
        }
    }

    val bushCallerRepository = object : BushCallerRepository(podDiscovery) {
        override fun create(bushKey: BushKey, podKey: PodKey): BushCaller = bushCaller(podKey)
    }

    val podProxy = object : MergingPodProxy<Sample, SampleArray>(
            forPartition = 0,
            bushCallerRepository = bushCallerRepository,
            podDiscovery = podDiscovery,
            converter = { it.nullableSampleArrayList() },
            elementExtractor = { arr, i -> if (i < arr.size) arr[i] else null },
            prefetchBucketAmount = timeToReadAtOnce,
            partitionSize = partitionSize
    ) {
        override val readsFrom = this@MergingPodProxyTester.readsFrom.map { it.podKey }

    }
}


@UseExperimental(ExperimentalStdlibApi::class)
object MergingPodProxySpec : Spek({

    describe("Merging Pod Proxy for two Pods with partitionSize=2") {
        val podProxyTester = MergingPodProxyTester(
                readsFrom = listOf(
                        newTestStreamingPod((0..11).filter { it % 4 in setOf(0, 1) }, partition = 0, partitionSize = 2),
                        newTestStreamingPod((0..11).filter { it % 4 in setOf(2, 3) }, partition = 1, partitionSize = 2)
                ),
                timeToReadAtOnce = 2,
                partitionSize = 2
        )

        val seq = podProxyTester.podProxy.asSequence(20.0f)
        it("should create a sequence") { assertThat(seq).isNotNull() }
        it("should call iteratorStart twice") { assertThat(podProxyTester.iteratorStartCounter).isEqualTo(2) }
        val res = seq.take(12).map { it.asInt() }.toList()
        it("should read all samples") { assertThat(res).isEqualTo((0..11).toList()) }
        it("should call iteratorNext 4 times") {
            assertThat(podProxyTester.iteratorNextCounter).isEqualTo(4)
        }
    }

    describe("Merging Pod Proxy for two Pods with partitionSize=1") {
        val podProxyTester = MergingPodProxyTester(
                readsFrom = listOf(
                        newTestStreamingPod((1..11 step 2).toList(), partition = 0),
                        newTestStreamingPod((2..12 step 2).toList(), partition = 1)
                ),
                timeToReadAtOnce = 3,
                partitionSize = 1
        )

        val seq = podProxyTester.podProxy.asSequence(20.0f)
        it("should create a sequence") { assertThat(seq).isNotNull() }
        it("should call iteratorStart twice") { assertThat(podProxyTester.iteratorStartCounter).isEqualTo(2) }
        val res = seq.take(12).map { it.asInt() }.toList()
        it("should read all samples") { assertThat(res).isEqualTo((1..12).toList()) }
        it("should call iteratorNext 4 times") {
            assertThat(podProxyTester.iteratorNextCounter).isEqualTo(4)
        }
    }
})