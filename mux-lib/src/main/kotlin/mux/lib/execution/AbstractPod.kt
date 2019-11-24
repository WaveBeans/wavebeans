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
        private val newIteratorMutex = Any()
    }

    @Volatile
    private var iterator: Iterator<Any>? = null
    @Volatile
    private var partitionIdx = 0

    private val buffers = ConcurrentHashMap<Long, Pair<Int, Queue<Any>>>()
    private val locks = ConcurrentHashMap<Long, ReentrantLock>()


    @Suppress("unused")
    private val prefetchingThread = Thread {
        var i = 0
        while (true) {
            val biggestBufferSize = buffers.values.map { it.second.size }.max() ?: 0
            var iterations = maxPrefetchAmount - biggestBufferSize

            while (iterations > 0) {
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

    override fun iteratorStart(sampleRate: Float, partitionIdx: Int): Long {
        val key = idSeq.incrementAndGet()
        buffers[key] = Pair(partitionIdx, ConcurrentLinkedQueue())
        locks[key] = ReentrantLock()
        if (iterator == null) {
            synchronized(newIteratorMutex) {
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

        // only one thread can read from the buffer itself
        check(lock.tryLock(1000, TimeUnit.MILLISECONDS)) { "POD[$podKey][iteratorKey=$iteratorKey, forPartition=$forPartition] Can't acquire lock" }
        try {
            if (buf.size < buckets) { // not enough data
                synchronized(iterator!!) {
                    var iterations = buckets - buf.size
                    while (iterations > 0) {
                        if (iterator!!.hasNext()) {
                            // read the next element from the stream and spread across consumer queues
                            val el = iterator!!.next()
                            buffers
                                    .filter { partitionCount == 1 || it.value.first == partitionIdx }
                                    .forEach { it.value.second.add(el) }
                            partitionIdx = (partitionIdx + 1) % partitionCount
                        } else {
                            break
                        }
                        iterations--
                    }
                }
            }

            val elements = (0 until min(buckets, buf.size)).mapNotNull { buf.poll() }
            return if (elements.isEmpty()) null else elements
        } finally {
            lock.unlock()
        }
    }

    override fun close() {
        println("POD[$podKey] Closed")
    }

    override fun isFinished(): Boolean = true
}