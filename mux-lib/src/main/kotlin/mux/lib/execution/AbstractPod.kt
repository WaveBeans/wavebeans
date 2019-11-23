package mux.lib.execution

import mux.lib.BeanStream
import java.lang.Thread.sleep
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

// ThreadSafe
abstract class AbstractPod(
        override val podKey: PodKey,
        val bean: BeanStream<*, *>,
        val partitionCount: Int,
        val maxPrefetchAmount: Int = 1024
) : Pod {

    companion object {
        private val idSeq = AtomicLong(0)
        private val newIteratorMutex = ConcurrentHashMap<PodKey, Any>()
    }

    @Volatile
    private var iterator: Iterator<Any>? = null

    private val buffers = ConcurrentHashMap<Long, Pair<Int, Queue<Any>>>()
    private val locks = ConcurrentHashMap<Long, ReentrantLock>()

    @Volatile
    private var prefetchingThreadIsRunning = true

    @Volatile
    private var firstConsumerCameBy = false

    @Suppress("unused")
    private val prefetchingThread = Thread {
        var partitionIdx = 0
        // do not start prefetching before clients start to really read
        // that allows to establish more than one parallel iterator
        while (prefetchingThreadIsRunning && !firstConsumerCameBy) sleep(0)

        // wait for all partitions to start streaming
        println("POD[$podKey] Started fetching [prefetchingThreadIsRunning=$prefetchingThreadIsRunning, " +
                "firstConsumerCameBy=$firstConsumerCameBy, active iterators key:partition=${buffers.map { it.key to it.value.first }}]")
        var i = 0
        while (prefetchingThreadIsRunning) {
            val biggestBufferSize = buffers.values.map { it.second.size }.max() ?: 0
            var iterations = maxPrefetchAmount - biggestBufferSize

            while (prefetchingThreadIsRunning && iterations > 0) {
                if (iterator != null) {
                    if (iterator!!.hasNext()) {
                        // read the next element from the stream and spread across consumer queues
                        val el = iterator!!.next()
                        buffers
                                .filter { partitionCount == 1 || it.value.first == partitionIdx }
                                .forEach { it.value.second.add(el) }
                        partitionIdx = (partitionIdx + 1) % partitionCount
                    } else {
                        // nothing more to read, stop the prefetch
                        prefetchingThreadIsRunning = false
                        break
                    }
                }
                iterations--
            }
            i++
            sleep(0)
        }
        println("POD[$podKey] Prefetching thread finished.")
    }
            .also { it.name = "Prefetch-$podKey" }
            .also { it.start() }

    override fun iteratorStart(sampleRate: Float, partitionIdx: Int): Long {
        val key = idSeq.incrementAndGet()
        buffers[key] = Pair(partitionIdx, ArrayBlockingQueue(maxPrefetchAmount))
        locks[key] = ReentrantLock()
        newIteratorMutex.putIfAbsent(podKey, Any())
        if (iterator == null) {
            synchronized(newIteratorMutex[podKey]!!) {
                if (iterator == null) iterator = bean.asSequence(sampleRate).iterator()
            }
        }
        return key
    }

    override fun iteratorNext(iteratorKey: Long, buckets: Int): List<Any>? {
        val buf = buffers[iteratorKey]?.second
        val lock = locks[iteratorKey]
        check(buf != null && lock != null) { "Iterator key $iteratorKey is unknown for the pod $this" }
        val forPartition = buffers[iteratorKey]?.first

        firstConsumerCameBy = true
        // only one thread can read from the buffer itself
        check(lock.tryLock(1000, TimeUnit.MILLISECONDS)) { "POD[$podKey][iteratorKey=$iteratorKey, forPartition=$forPartition] Can't acquire lock" }
        try {
            // make sure we have all data prefetched
            while (prefetchingThreadIsRunning && buf.size < buckets) sleep(0)

            val elements = (0 until min(buckets, buf.size)).mapNotNull { buf.poll() }
            return if (elements.isEmpty()) null else elements
        } finally {
            lock.unlock()
        }
    }

    override fun close() {
        prefetchingThreadIsRunning = false
        println("POD[$podKey] Closed")
    }

    override fun isFinished(): Boolean = !prefetchingThreadIsRunning
}