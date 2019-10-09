package mux.lib.execution

import mux.lib.BeanStream
import mux.lib.timeToSampleIndexFloor
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.collections.ArrayList
import kotlin.math.max
import kotlin.math.min

// the class is not thread safe
abstract class StreamingPod<T : Any, S : Any>(
        override val podKey: PodKey,
        val unclaimedElementsCleanupThreshold: Int = 1024
) : Pod<T, S>, BeanStream<T, S> {

    companion object {
        private val idSeq = AtomicLong(0)
    }

    class PodIterator<T>(
            val iterator: Iterator<T>,
            val sampleRate: Float
    )

    private var iterator: PodIterator<T>? = null

    private val offsets = HashMap<Long, Long>()
    private var buffer = ArrayList<T>()
    private var globalOffset = 0L

    fun iteratorStart(sampleRate: Float): Long {
        val key = idSeq.incrementAndGet()
        offsets[key] = globalOffset
        if (iterator == null) // TODO handle different sample rate?
            iterator = PodIterator(asSequence(sampleRate).iterator(), sampleRate)
        return key
    }

    fun iteratorNext(iteratorKey: Long, length: Long, timeUnit: TimeUnit): List<T>? {
        val pi = iterator
        check(pi != null) { "Pod wasn't initialized properly. Iterator not found. Call `start` first." }
        val offset = offsets[iteratorKey]
        check(offset != null) { "Iterator key $iteratorKey is unknown for the pod $this" }

        val bufferPos = max(offset - globalOffset, 0L).toInt()
        val samplesCount = timeToSampleIndexFloor(length, timeUnit, pi.sampleRate).toInt()

        // if we'retrying to read out of buffer -- try to read from downstream iterator
        while (bufferPos + samplesCount >= buffer.size) {
            if (pi.iterator.hasNext()) {
                val element = pi.iterator.next()
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
            val sampleToRead = min(buffer.size, bufferPos + samplesCount)

            // make a copy, as sublist is just view of the list which later will be cleaned
            val elements = buffer.subList(bufferPos, sampleToRead).toList()

            offsets[iteratorKey] = offset + sampleToRead

            // if all iterators consumed data, push the global offset forward and drop the unneeded data from buffer
            val newGlobalOffset = offsets.values.min() ?: 0
            val currentGlobalOffset = globalOffset
            val unclaimedElements = (newGlobalOffset - currentGlobalOffset).toInt()
            if (unclaimedElements >= unclaimedElementsCleanupThreshold) {
                buffer = ArrayList(buffer.subList(unclaimedElements, buffer.size))
                globalOffset = newGlobalOffset
            }

            elements
        }
    }

    override fun toString(): String = "[$podKey]${this::class.simpleName}"
}