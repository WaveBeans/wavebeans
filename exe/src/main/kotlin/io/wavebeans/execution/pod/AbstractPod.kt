package io.wavebeans.execution.pod

import io.wavebeans.lib.BeanStream
import mu.KotlinLogging
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock
import kotlin.math.min

// TODO consider moving to config
const val DEFAULT_PARTITION_SIZE = 512

typealias TransferContainer = Any

/**
 * Base implementation of the [Pod].
 *
 * **Supports partitioning**. If [partitionCount] is greater than 1, then [Pod] will attempt to partition data.
 * It is implemented via windowing with the size=[partitionSize] and step=[partitionSize]. Each batch is packed into array via
 * [converter] function.
 *
 * **Thread safe**.
 * There a a few main points that are required to be thread safe:
 *
 * 1. Underlying iterator creation. We need to make sure that underlying bean is not read more than once.
 * For that purpose mutex and double check locking is used.
 * 2. Reading from the specific iterator buffer. As buffer is filled lazily when it's needed, need to make sure the same
 * iterator doesn't try to do the same and the buffer will over-read the data.That is critical as partitioning is purely
 * based on indexes and disobeying this rule may result in shift of the partition offset.
 * It is achieved with simple [ReentrantLock] on per iterator basis.
 * 3. The read operations is also in critical section, however each iteration supplies data for all iterators.
 * We just need to make sure this operation happens sequentially. The same mutex object is used as to create the iterator for pod.
 *
 * @param podKey The [PodKey] the pod is registered under. It is used mainly for logging purposes.
 * @param bean The [io.wavebeans.lib.Bean] to stream from. Can work only with [BeanStream] which are exposing iterator interface.
 * @param partitionCount The number of partition this pod is handling at the same time. It should be at least one.
 * @param converter the function that converts values to an array (not actually checked and required, it's just an intent),
 *      mainly to be able to use primitive arrays during transfer
 * @param partitionSize the size of single partition. Must correspond to the value provided for [PodProxyIterator.partitionSize]
 *      via instantiation of any [PodProxy]
 */
// ThreadSafe
abstract class AbstractPod<T : Any, B : BeanStream<T>>(
        override val podKey: PodKey,
        val bean: B,
        val partitionCount: Int,
        val converter: (List<T>) -> TransferContainer,
        val partitionSize: Int = DEFAULT_PARTITION_SIZE
) : Pod {

    companion object {
        private val idSeq = AtomicLong(0)
        private val log = KotlinLogging.logger { }
    }


    /**
     * Is used inside [iteratorStart] and [iteratorNext] to make sure the iterator is accessed within only one thread.
     */
    private val iteratorLock = ReentrantLock()

    /**
     * [bean] iterator or `null` if it's not initialized yet. Initialized lazily during first call to [Pod]
     */
    @Volatile
    private var iterator: Iterator<T>? = null

    /**
     * Point to the partition index that will be read next. The value is in range `[0, partitionCount)`
     */
    @Volatile
    private var partitionIdx = 0

    /**
     * The buffers for all iterators. All buffers are represented as Queues, so the producer appends data to the end,
     * and consumer polls data from the head. The queue currently is unlimited.
     *
     * * The `key` is iterator key
     * * The `value` is pair of partition index and buffer (as queue) itself
     */
    private val buffers = ConcurrentHashMap<Long, Pair<Int, Queue<Any>>>()

    /**
     * The set of iterator locks. The `key` is iterator key, the value is [ReentrantLock] which is used
     * with timeout=1000ms during [iteratorNext]
     */
    private val locks = ConcurrentHashMap<Long, ReentrantLock>()

    override fun iteratorStart(sampleRate: Float, partitionIdx: Int): Long {
        val key = idSeq.incrementAndGet()

        log.trace { "[POD=$podKey] iteratorStart?sampleRate=$sampleRate&partitionIdx=$partitionIdx [key=$key]" }

        buffers[key] = Pair(partitionIdx, ConcurrentLinkedQueue()) // TODO use different queue
        locks[key] = ReentrantLock()
        if (iterator == null) {
            iteratorLock.lock()
            try {
                if (iterator == null) iterator = bean.asSequence(sampleRate).iterator()
            } finally {
                iteratorLock.unlock()
            }
        }
        return key
    }

    override fun iteratorNext(iteratorKey: Long, buckets: Int): List<Any>? {
        val buf = buffers[iteratorKey]?.second
        val lock = locks[iteratorKey]
        check(buf != null && lock != null) { "Iterator key $iteratorKey is unknown for the pod $this" }

        log.trace {
            "Starting [POD=$podKey] iteratorNext?iteratorKey=$iteratorKey&buckets=$buckets " +
                    "[buf.size=${buf.size}, forPartition=${buffers[iteratorKey]?.first}], " +
                    "lock=$lock, iteratorLock=$iteratorLock"
        }

        // only one thread can read from the buffer itself
        check(lock.tryLock(1000, TimeUnit.MILLISECONDS)) {
            "POD[$podKey][iteratorKey=$iteratorKey, forPartition=${buffers[iteratorKey]?.first}] Can't acquire lock"
        }
        try {
            val i = iterator!!
            if (buf.size < buckets) { // not enough data
                // can't check if there are some elements in the iterator as it has side effect of starting fetching element
                // avoid reading from different iterators
                iteratorLock.lock()
                try {
                    if (buf.size < buckets && i.hasNext()) { // double check, if that wasn't read before
                        var iterations = buckets * partitionSize * partitionCount - buf.size * partitionSize // TODO should it make sure the iterations is divisible by partition size?
                        log.trace { "Reading... [POD=$podKey] Doing $iterations iterations to fill the buffer [buf.size=${buf.size}]" }
                        var list = ArrayList<T>(partitionSize)
                        var leftToReadForPartition = partitionSize
                        while (iterations >= 0) {
                            if (i.hasNext()) {
                                // read the next batch from the stream and spread across consumer queues
                                if (leftToReadForPartition == 0 || iterations == 0) {
                                    // if partition is full or no iterations left -- dump the partition to the buffer
                                    buffers
                                            .filter { partitionCount == 1 || it.value.first == partitionIdx }
                                            .forEach { it.value.second.add(converter(list)) }
                                    partitionIdx = (partitionIdx + 1) % partitionCount
                                    list = ArrayList(partitionSize)
                                    leftToReadForPartition = partitionSize
                                }
                                // if that's not the last iteration, for the last iteration it wouldn't
                                // have any buffer to pick it up
                                if (iterations > 0) {
                                    list.add(i.next())
                                    leftToReadForPartition--
                                }
                            } else {
                                if (list.isNotEmpty()) {
                                    buffers
                                            .filter { partitionCount == 1 || it.value.first == partitionIdx }
                                            .forEach { it.value.second.add(converter(list)) }
                                }
                                break
                            }
                            iterations--
                        }
                        log.trace {
                            "Reading... [POD=$podKey] Left $iterations iterations after filling the buffer, hasNext=${i.hasNext()}, " +
                                    "buffers=${buffers.map { Triple("iteratorKey="+it.key, "partitionIndex="+it.value.first, "bufferSize="+it.value.second.size) }}"
                        }
                    }
                } finally {
                    iteratorLock.unlock()
                }
            }

            val elements = (0 until min(buckets, buf.size)).mapNotNull { buf.poll() }
            log.trace {
                "Returning [POD=$podKey] iteratorNext?iteratorKey=$iteratorKey&buckets=$buckets " +
                        "[elements.size=${elements.size}]"// + "\n${elements.map { it as SampleArray }.flatMap { it.asList() }}"
            }
            return if (elements.isEmpty()) null else elements
        } finally {
            lock.unlock()
        }
    }

    override fun close() {
        log.debug { "POD[$podKey] Closed" }
    }

    override fun isFinished(): Boolean = true // TODO get rid of this method?
}