package io.wavebeans.execution.podproxy

import io.wavebeans.execution.BushCallerRepository
import io.wavebeans.execution.PodDiscovery
import io.wavebeans.execution.medium.PodCallResult
import io.wavebeans.execution.medium.long
import io.wavebeans.execution.pod.DEFAULT_PARTITION_SIZE
import io.wavebeans.execution.pod.PodKey
import mu.KotlinLogging
import java.util.concurrent.TimeUnit

// TODO consider providing via Config
const val DEFAULT_PREFETCH_BUCKET_AMOUNT = 10

class PodProxyIterator<T : Any, ARRAY_T>(
        val sampleRate: Float,
        val pod: PodKey,
        val readingPartition: Int,
        val podDiscovery: PodDiscovery = PodDiscovery.default,
        val bushCallerRepository: BushCallerRepository = BushCallerRepository.default(podDiscovery),
        val converter: (PodCallResult) -> List<ARRAY_T>?,
        val elementExtractor: (ARRAY_T, Int) -> T?,
        val prefetchBucketAmount: Int = DEFAULT_PREFETCH_BUCKET_AMOUNT,
        val partitionSize: Int = DEFAULT_PARTITION_SIZE
) : Iterator<T> {

    companion object {
        private val log = KotlinLogging.logger { }
    }

    private val bush = podDiscovery.bushFor(pod)
    private val caller = bushCallerRepository.create(bush, pod)
    private val iteratorKey: Long

    init {
        iteratorKey = caller.call("iteratorStart?sampleRate=$sampleRate&partitionIdx=${readingPartition}")
                .get(5000, TimeUnit.MILLISECONDS).long()
        log.trace { "Created iterator [Pod=$pod] iteratorKey=$iteratorKey" }
    }

    private var buckets: List<ARRAY_T>? = null
    private var bucketPointer = 0
    private var pointer = 0
    private var nextEl: T? = null

    override fun hasNext(): Boolean {
        if (nextEl != null) return true
        val s = tryReadBuckets()
        return if (s != null
                && s.isNotEmpty()
                && bucketPointer < s.size
                && pointer < partitionSize) {
            return tryReadNextEl()
        } else {
            false
        }
    }

    override fun next(): T {
        if (nextEl != null) {
            val el = nextEl!!
            nextEl = null
            return el
        }
        if (tryReadNextEl()) {
            val el = nextEl
            nextEl = null
            return el!!
        } else {
            throw NoSuchElementException("Pod $pod has no more buckets")
        }
    }

    private fun tryReadBuckets(): List<ARRAY_T>? {
        if (buckets == null || bucketPointer >= buckets?.size ?: 0) {
            log.trace { "Calling iterator [Pod=$pod] iteratorKey=$iteratorKey" }

            val podResult = caller.call(
                    "iteratorNext" +
                            "?iteratorKey=$iteratorKey" +
                            "&buckets=$prefetchBucketAmount"
            ).get(5000, TimeUnit.MILLISECONDS)

            if (podResult.isNull()) {
                buckets = null
                bucketPointer = 0
                pointer = 0
                return null
            }
            buckets = converter(podResult)
            bucketPointer = 0
            pointer = 0
        }
        return buckets
    }

    private fun tryReadNextEl(): Boolean {
        val s = tryReadBuckets() ?: return false
        val bucket = s[bucketPointer]
        val el = elementExtractor(bucket, pointer) ?: return false

        if (++pointer >= partitionSize) {
            pointer = 0
            bucketPointer++
        }

        nextEl = el
        return true
    }
}