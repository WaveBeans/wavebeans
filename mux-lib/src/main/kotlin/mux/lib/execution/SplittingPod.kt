package mux.lib.execution

import mux.lib.BeanStream
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import kotlin.collections.ArrayList
import kotlin.collections.set
import kotlin.math.max

abstract class SplittingPod<T : Any, S : Any>(
        override val podKey: PodKey,
        val partitionCount: Int,
        val unburdenElementsCleanupThreshold: Int = 1024
) : Pod<T, S>, BeanStream<T, S> {

    companion object {
        private val idSeq = AtomicLong(0)
    }

    private var globalOffset = 0L
    private val offsets = HashMap<Long, Long>()
    private var iterator: Iterator<T>? = null
    private var buffer = ArrayList<T>(unburdenElementsCleanupThreshold * partitionCount * 2)

    fun iteratorStart(sampleRate: Float, partitionIdx: Int): Long {
        val key = idSeq.incrementAndGet()
        offsets[key] = globalOffset / partitionCount * partitionCount + partitionIdx
        if (iterator == null) // TODO handle different sample rate?
            iterator = asSequence(sampleRate).iterator()
        return key
    }

    fun iteratorNext(iteratorKey: Long, buckets: Int): List<T>? {
        val pi = iterator
        check(pi != null) { "Pod wasn't initialized properly. Iterator not found. Call `start` first." }
        val offset = offsets[iteratorKey]
        check(offset != null) { "Iterator key $iteratorKey is unknown for the pod $this" }

        val bufferPos = max(offset - globalOffset, 0L).toInt()
        val toHaveBuffered = buckets * partitionCount

        // if we're trying to read out of buffer -- try to read first from downstream iterator
        while (bufferPos + toHaveBuffered >= buffer.size) {
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
            // otherwise the elements should be there
            val elements = ArrayList<T>(buckets)
            var read = 1
            var idx = bufferPos
            do {
                elements.add(buffer[idx])
                idx += partitionCount
            } while (idx < buffer.size && read++ < buckets)
            offsets[iteratorKey] = offset + idx - bufferPos


            // if all iterators consumed data, push the global offset forward and drop the unneeded data from buffer
            val newGlobalOffset = offsets.values.min() ?: 0
            val unburdenElements = (newGlobalOffset - globalOffset).toInt()
            if (unburdenElements >= unburdenElementsCleanupThreshold) {
                println("Before clean up: ${buffer.size}")
                if (unburdenElements < buffer.size) {
                    buffer = ArrayList(buffer.subList(unburdenElements, buffer.size))
                    globalOffset = newGlobalOffset
                } else {
                    buffer = ArrayList(unburdenElementsCleanupThreshold * 2)
                    globalOffset = newGlobalOffset
                }
                println("Cleaned up, new buffer: ${buffer.size}")
            }

            elements
        }
    }

    override fun toString(): String = "[$podKey]${this::class.simpleName}<SplitTo=${partitionCount}>"

}