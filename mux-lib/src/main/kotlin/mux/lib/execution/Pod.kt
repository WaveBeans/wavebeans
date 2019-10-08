package mux.lib.execution

import mux.lib.Bean
import mux.lib.BeanParams
import mux.lib.BeanStream
import mux.lib.Sample
import java.lang.reflect.InvocationTargetException
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.NoSuchElementException
import kotlin.math.max

typealias PodKey = Int
typealias AnyPod = Pod<*, *>

interface Pod<T : Any, S : Any> : Bean<T, S> {

    val podKey: PodKey

    @ExperimentalStdlibApi
    fun call(call: Call): PodCallResult {
        return try {
            val method = this::class.members
                    .firstOrNull { it.name == call.method }
                    ?: throw IllegalStateException("Can't find method to call: $call")
            val params = method.parameters
                    .drop(1) // drop self instance
                    .map {
                        call.param(
                                key = it.name
                                        ?: throw IllegalStateException("Parameter `$it` of method `$method` has no name"),
                                type = it.type)
                    }
                    .toTypedArray()

            val result = method.call(this, *params)
            PodCallResult.wrap(call, result)
        } catch (e: InvocationTargetException) {
            PodCallResult.wrap(call, e.targetException)
        } catch (e: Throwable) {
            PodCallResult.wrap(call, e)
        }
    }
}

interface PodProxy<T : Any, S : Any> : Bean<T, S> {

    val pointedTo: PodKey
}

// the class is not thread safe
abstract class StreamingPod<T : Any, S : Any>(override val podKey: PodKey) : Pod<T, S>, BeanStream<T, S> {

    companion object {
        private val idSeq = AtomicLong(0)
    }

    private var iterator: Iterator<T>? = null

    private val offsets = HashMap<Long, Long>()
    private val buffer = LinkedList<T>()
    private var globalOffset = 0L

    fun iteratorStart(sampleRate: Float): Long {
        val key = idSeq.incrementAndGet()
        offsets[key] = globalOffset
        if (iterator == null) // TODO handle different sample rate?
            iterator = asSequence(sampleRate).iterator()
        return key
    }

    fun iteratorNext(iteratorKey: Long, pushOffset: Boolean = true): T? {
        check(iterator != null) { "Pod wasn't initialized properly. Iterator not found. Call `start` first." }
        val offset = offsets[iteratorKey]
        check(offset != null) { "Iterator key $iteratorKey is unknown for the pod $this" }

        val bufferPos = max(offset - globalOffset, 0L).toInt()

        // if we'retrying to read out of buffer -- try to read from downstream iterator
        while (bufferPos >= buffer.size) {
            if (iterator!!.hasNext()) {
                val element = iterator!!.next()
                buffer.addLast(element)
            } else {
                break
            }
        }

        return if (bufferPos >= buffer.size) {
            // if nothing to read from down stream iterator, then nothing to return
            null
        } else {
            // otherwise the element should be there
            val el = buffer[bufferPos]
            offsets[iteratorKey] = offset + 1

            // if all iterators consumed data, push the global offset forward and drop the unneeded data from buffer
            if (pushOffset) {
                val newGlobalOffset = offsets.values.min() ?: 0
                val currentGlobalOffset = globalOffset
                repeat((newGlobalOffset - currentGlobalOffset).toInt()) {
                    buffer.removeFirst()
                }
                globalOffset = newGlobalOffset
            }

            el
        }
    }

    override fun toString(): String = "[$podKey]${this::class.simpleName}"
}

abstract class StreamingPodProxy<S : Any>(
        override val pointedTo: PodKey,
        val podDiscovery: PodDiscovery = PodDiscovery.default,
        val bushCallerRepository: BushCallerRepository = BushCallerRepository.default(podDiscovery)
) : BeanStream<Sample, S>, PodProxy<Sample, S> {

    override fun asSequence(sampleRate: Float): Sequence<Sample> {
        val bush = podDiscovery.bushFor(pointedTo)
        val caller = bushCallerRepository.create(bush, pointedTo)
        val iteratorKey = caller.call("iteratorStart?sampleRate=$sampleRate").long()

        return object : Iterator<Sample> {

            var sample: Sample? = null
            var readFirstSample = false

            override fun hasNext(): Boolean {
                if (sample == null && !readFirstSample) readSample()
                return sample != null
            }

            override fun next(): Sample {
                val oldSample = sample ?: throw NoSuchElementException("Pod $pointedTo has no samples")
                readSample()
                return oldSample
            }

            private fun readSample(pushOffset: Boolean = true) {
                readFirstSample = true
                sample = caller.call("iteratorNext?iteratorKey=$iteratorKey&pushOffset=$pushOffset").nullableSample()
            }
        }.asSequence()
    }

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): S = throw UnsupportedOperationException("That's not required for PodProxy")

    override fun inputs(): List<Bean<*, *>> = throw UnsupportedOperationException("That's not required for PodProxy")

    override val parameters: BeanParams
        get() = throw UnsupportedOperationException("That's not required for PodProxy")

    override fun toString(): String = "${this::class.simpleName}->[$pointedTo]"
}