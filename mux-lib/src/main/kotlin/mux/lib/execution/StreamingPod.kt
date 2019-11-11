package mux.lib.execution

import mux.lib.BeanStream
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import kotlin.collections.ArrayList
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.typeOf

// the class is not thread safe
abstract class StreamingPod<T : Any, S : Any>(
        override val podKey: PodKey,
        val unburdenElementsCleanupThreshold: Int = 1024
) : Pod<T, S>, BeanStream<T, S> {

    companion object {
        private val idSeq = AtomicLong(0)
    }

    private var iterator: Iterator<T>? = null

    private val offsets = HashMap<Long, Long>()
    private var buffer = ArrayList<T>(unburdenElementsCleanupThreshold * 2)
    private var globalOffset = 0L

    override fun iteratorStart(sampleRate: Float, partitionIdx: Int): Long {
        val key = idSeq.incrementAndGet()
        offsets[key] = globalOffset
        if (iterator == null) // TODO handle different sample rate?
            iterator = asSequence(sampleRate).iterator()
        return key
    }

    override fun iteratorNext(iteratorKey: Long, buckets: Int): List<T>? {
        val pi = iterator
        check(pi != null) { "Pod wasn't initialized properly. Iterator not found. Call `start` first." }
        val offset = offsets[iteratorKey]
        check(offset != null) { "Iterator key $iteratorKey is unknown for the pod $this" }

        val bufferPos = max(offset - globalOffset, 0L).toInt()

        // if we're trying to read out of buffer -- try to read first from downstream iterator
        while (bufferPos + buckets >= buffer.size) {
            if (pi.hasNext()) {
                val element = pi.next()
                buffer.add(element)
            } else {
                break
            }
        }

        return if (bufferPos >= buffer.size) {
            // if nothing to read from down stream iterator, then nothing to return
            null
        } else {
            // otherwise the element should be there
            val endOfBuffer = min(buffer.size, bufferPos + buckets)

            // make a copy, as sublist is just view of the list which later will be cleaned
            val elements = buffer.subList(bufferPos, endOfBuffer).toList()

            offsets[iteratorKey] = offset + (endOfBuffer - bufferPos)

            // if all iterators consumed data, push the global offset forward and drop the unneeded data from buffer
            val newGlobalOffset = offsets.values.min() ?: 0
            val unburdenElements = (newGlobalOffset - globalOffset).toInt()
            if (unburdenElements >= unburdenElementsCleanupThreshold) {
                if (unburdenElements < buffer.size) {
                    buffer = ArrayList(buffer.subList(unburdenElements, buffer.size))
                    globalOffset = newGlobalOffset
                } else {
                    buffer = ArrayList(unburdenElementsCleanupThreshold * 2)
                    globalOffset = newGlobalOffset
                }
            }

            elements
        }
    }

    override fun toString(): String = "[$podKey]${this::class.simpleName}"
}