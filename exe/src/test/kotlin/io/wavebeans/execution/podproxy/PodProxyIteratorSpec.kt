package io.wavebeans.execution.podproxy

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.nhaarman.mockitokotlin2.mock
import io.wavebeans.execution.*
import io.wavebeans.execution.medium.PodCallResult
import io.wavebeans.execution.medium.sampleArrayList
import io.wavebeans.execution.pod.Pod
import io.wavebeans.execution.pod.PodKey
import io.wavebeans.lib.ZeroSample
import io.wavebeans.lib.sampleOf
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

class PodProxyIteratorTester<T : Any, ARRAY_T : Any>(
        val pointedTo: Pod,
        converter: (PodCallResult) -> List<ARRAY_T>?,
        elementExtractor: (ARRAY_T, Int) -> T?,
        partitionSize: Int = 1,
        prefetchBucketAmount: Int = 1
) {
    val podDiscovery: PodDiscovery = mock()

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

    val iterator = PodProxyIterator(
            sampleRate = 1.0f,
            pod = pointedTo.podKey,
            readingPartition = 0,
            podDiscovery = podDiscovery,
            bushCallerRepository = bushCallerRepository,
            partitionSize = partitionSize,
            prefetchBucketAmount = prefetchBucketAmount
    )
}

object PodProxyIteratorSpec : Spek({
    describe("Partition size = 1, buckets = 1") {
        val iterator = PodProxyIteratorTester(
                pointedTo = newTestStreamingPod((1..2).toList()),
                converter = { r -> r.sampleArrayList() },
                elementExtractor = { arr, i -> if (i < arr.size) arr[i] else null }
        )

        it("should have some elements") { assertThat(iterator.iterator.hasNext()).isTrue() }
        it("should read first element") { assertThat(iterator.iterator.next()).isEqualTo(sampleOf(1)) }
        it("should read second element") { assertThat(iterator.iterator.next()).isEqualTo(sampleOf(2)) }
        it("should have no more elements") { assertThat(iterator.iterator.hasNext()).isFalse() }
    }

    describe("Partition size = 2, buckets = 1.") {
        val iterator = PodProxyIteratorTester(
                pointedTo = newTestStreamingPod((1..4).toList(), partitionSize = 2),
                converter = { r -> r.sampleArrayList() },
                elementExtractor = { arr, i -> if (i < arr.size) arr[i] else null },
                partitionSize = 2
        )

        it("should have some elements") { assertThat(iterator.iterator.hasNext()).isTrue() }
        it("should read first element") { assertThat(iterator.iterator.next()).isEqualTo(sampleOf(1)) }
        it("should have some elements") { assertThat(iterator.iterator.hasNext()).isTrue() }
        it("should read second element") { assertThat(iterator.iterator.next()).isEqualTo(sampleOf(2)) }
        it("should have some elements") { assertThat(iterator.iterator.hasNext()).isTrue() }
        it("should read second element") { assertThat(iterator.iterator.next()).isEqualTo(sampleOf(3)) }
        it("should have some elements") { assertThat(iterator.iterator.hasNext()).isTrue() }
        it("should read second element") { assertThat(iterator.iterator.next()).isEqualTo(sampleOf(4)) }
        it("should have no more elements") { assertThat(iterator.iterator.hasNext()).isFalse() }
    }

    describe("Partition size = 2, buckets = 1. Not enough data to fill in full buckets") {
        val iterator = PodProxyIteratorTester(
                pointedTo = newTestStreamingPod((1..3).toList(), partitionSize = 2),
                converter = { r -> r.sampleArrayList() },
                elementExtractor = { arr, i -> if (i < arr.size) arr[i] else null },
                partitionSize = 2
        )

        it("should have some elements") { assertThat(iterator.iterator.hasNext()).isTrue() }
        it("should read first element") { assertThat(iterator.iterator.next()).isEqualTo(sampleOf(1)) }
        it("should read second element") { assertThat(iterator.iterator.next()).isEqualTo(sampleOf(2)) }
        it("should read second element") { assertThat(iterator.iterator.next()).isEqualTo(sampleOf(3)) }
        it("should have no more elements") { assertThat(iterator.iterator.hasNext()).isFalse() }
    }
})