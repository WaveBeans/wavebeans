package mux.lib.execution

class PodProxyIterator<T>(
        val sampleRate: Float,
        val pod: PodKey,
        val readingPartition: Int,
        val podDiscovery: PodDiscovery = PodDiscovery.default,
        val bushCallerRepository: BushCallerRepository = BushCallerRepository.default(podDiscovery),
        val converter: (PodCallResult) -> List<T>?,
        val prefetchBucketAmount: Int = 10
): Iterator<T> {

    private val bush = podDiscovery.bushFor(pod)
    private val caller = bushCallerRepository.create(bush, pod)
    private val iteratorKey = caller.call("iteratorStart?sampleRate=$sampleRate&partitionIdx=${readingPartition}").long()

    private var buckets: List<T>? = null
    private var pointer = 0

    override fun hasNext(): Boolean {
        val s = tryReadBuckets()
        return s != null && s.isNotEmpty() && pointer < s.size
    }

    override fun next(): T {
        val s = tryReadBuckets()
                ?: throw NoSuchElementException("Pod $pod has no more buckets")
        return s[pointer++]
    }

    private fun tryReadBuckets(): List<T>? {
        if (buckets == null || pointer >= buckets?.size ?: 0) {
            buckets = converter(caller.call(
                    "iteratorNext" +
                            "?iteratorKey=$iteratorKey" +
                            "&buckets=$prefetchBucketAmount"
            ))
            pointer = 0
        }
        return buckets
    }
}