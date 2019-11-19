package mux.lib.execution

import mux.lib.BeanStream
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.min

// ThreadSafe
abstract class AbstractPod(
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

    private val buffers = ConcurrentHashMap<Long, Pair<Int, ArrayBlockingQueue<Any>>>()

    @Volatile
    private var prefetchingThreadIsRunning = true

    @Volatile
    private var waitForFirstConsume = true

    @Suppress("unused")
    private val prefetchingThread = Thread {
        var partitionIdx = 0
        // do not start prefetching before clients start to really read
        // that allows to establish more than one parallel iterator
        while (waitForFirstConsume) Thread.sleep(0)
        while (prefetchingThreadIsRunning) {
            val biggestBufferSize = buffers.values.map { it.second.size }.max() ?: 0
            var iterations = maxPrefetchAmount - biggestBufferSize
            while (iterations > 0) {
                if (iterator != null) {
                    if (iterator!!.hasNext()) {
                        // read the next element from the stream and spread across consumer queues
                        val el = iterator!!.next()
                        buffers
                                .filter { it.value.first == partitionIdx }
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
            Thread.sleep(0)
        }
    }
            .also { it.name = "Prefetch-$podKey" }
            .also { it.start() }

    override fun iteratorStart(sampleRate: Float, partitionIdx: Int): Long {
        val key = idSeq.incrementAndGet()
        buffers[key] = Pair(partitionIdx, ArrayBlockingQueue(maxPrefetchAmount))
        newIteratorMutex.putIfAbsent(podKey, Any())
        if (iterator == null) {
            synchronized(newIteratorMutex[podKey]!!) {
                if (iterator == null) iterator = bean.asSequence(sampleRate).iterator()
            }
        }
        return key
    }

    override fun iteratorNext(iteratorKey: Long, buckets: Int): List<Any>? {
        val pi = iterator
        check(pi != null) { "Pod wasn't initialized properly. Iterator not found. Call `start` first." }
        val buf = buffers[iteratorKey]?.second
        check(buf != null) { "Iterator key $iteratorKey is unknown for the pod $this" }

        waitForFirstConsume = false
        // only one thread can read from the buffer itself
        synchronized(buf) {
            // make sure we have all data prefetched
            while (prefetchingThreadIsRunning && buf.size < buckets) Thread.sleep(0)

            val elements = (0 until min(buckets, buf.size)).map { buf.take() }
            return if (elements.isEmpty()) null else elements
        }
    }

    override fun close() {
        prefetchingThreadIsRunning = false
    }

}