package io.wavebeans.execution.podproxy

import io.wavebeans.execution.BushCallerRepository
import io.wavebeans.execution.PodDiscovery
import io.wavebeans.execution.medium.Medium
import io.wavebeans.execution.medium.value
import io.wavebeans.execution.pod.DEFAULT_PARTITION_SIZE
import io.wavebeans.execution.pod.PodKey
import mu.KotlinLogging
import java.util.concurrent.TimeUnit

// TODO consider providing via Config
const val DEFAULT_PREFETCH_BUCKET_AMOUNT = 10

class PodProxyIterator(
        val sampleRate: Float,
        val pod: PodKey,
        val readingPartition: Int,
        val podDiscovery: PodDiscovery = PodDiscovery.default,
        val bushCallerRepository: BushCallerRepository = BushCallerRepository.default(podDiscovery),
        val prefetchBucketAmount: Int = DEFAULT_PREFETCH_BUCKET_AMOUNT,
        val partitionSize: Int = DEFAULT_PARTITION_SIZE
) : Iterator<Any> {

    companion object {
        private val log = KotlinLogging.logger { }
    }

    private val bush = podDiscovery.bushFor(pod)
    private val caller = bushCallerRepository.create(bush, pod)
    private val iteratorKey: Long

    init {
        iteratorKey = caller.call("iteratorStart?sampleRate=$sampleRate&partitionIdx=${readingPartition}")
                .get(5000, TimeUnit.MILLISECONDS).value()
        log.trace { "Created iterator [Pod=$pod] iteratorKey=$iteratorKey" }
    }

    private var buckets: List<Medium>? = null
    private var bucketPointer = 0
    private var pointer = 0
    private var nextEl: Any? = null

    override fun hasNext(): Boolean {
        if (nextEl != null) return true
        return tryReadNextEl()
    }

    override fun next(): Any {
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

    private fun tryReadBuckets(): List<Medium>? {
        if (buckets == null || bucketPointer >= buckets?.size ?: 0) {
            log.trace { "[$this] Calling iteratorNext(pod=$pod, iteratorKey=$iteratorKey)" }

            val podCallResult = caller.call(
                    "iteratorNext" +
                            "?iteratorKey=$iteratorKey" +
                            "&buckets=$prefetchBucketAmount"
            ).get(5000, TimeUnit.MILLISECONDS)

            if (podCallResult.exception != null) {
                throw IllegalStateException("Error while trying to read next bucket [iteratorKey=$iteratorKey,buckets=$buckets]", podCallResult.exception)
            }
            if (podCallResult.isNull()) {
                log.trace { "[$this] iteratorNext(pod=$pod, iteratorKey=$iteratorKey) returned null" }
                buckets = null
                bucketPointer = 0
                pointer = 0
                return null
            }
            buckets = podCallResult.value()
            log.trace { "[$this] iteratorNext(pod=$pod, iteratorKey=$iteratorKey) result was converted to buckets=$buckets" }
            bucketPointer = 0
            pointer = 0
        }
        return buckets
    }

    private fun tryReadNextEl(): Boolean {
        val s = tryReadBuckets() ?: return false
        val bucket = s[bucketPointer]
        val el = bucket.extractElement(pointer) ?: return false

        if (++pointer >= partitionSize) {
            pointer = 0
            bucketPointer++
        }

        nextEl = el
        return true
    }
}
