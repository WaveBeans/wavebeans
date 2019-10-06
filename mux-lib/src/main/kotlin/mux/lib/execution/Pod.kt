package mux.lib.execution

import mux.lib.Bean
import mux.lib.BeanParams
import mux.lib.BeanStream
import mux.lib.Sample
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

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

abstract class StreamingPod<T : Any, S : Any>(override val podKey: PodKey): Pod<T, S>, BeanStream<T, S> {

    companion object {
        private val idSeq = AtomicLong(0)
    }

    private class PodIterator<T>(
            val iterator: Iterator<T>
    )

    private val iterators = ConcurrentHashMap<Long, PodIterator<T>>()

    fun iteratorStart(sampleRate: Float): Long {
        val key = idSeq.incrementAndGet()
        iterators[key] = PodIterator(asSequence(sampleRate).iterator())
        return key
    }

    fun iteratorNext(iteratorKey: Long): T? {
        val i = iterators.getValue(iteratorKey).iterator
        if (i.hasNext())
            return i.next()
        else
            throw IllegalStateException("No elements left for iterator $iteratorKey")
    }

    override fun toString(): String = "${this::class.simpleName}[$podKey]"
}

abstract class AbstractStreamPodProxy<S : Any>(override val pointedTo: PodKey) : BeanStream<Sample, S>, PodProxy<Sample, S> {

    override fun asSequence(sampleRate: Float): Sequence<Sample> {
        val bush = PodDiscovery.bushFor(pointedTo)
        val caller = BushCaller(bush, pointedTo)
        val iteratorKey = caller.call("iteratorStart?sampleRate=$sampleRate").long()

        return object : Iterator<Sample> {

            var sample = readSample()

            override fun hasNext(): Boolean = sample != null

            override fun next(): Sample {
                val oldSample = sample ?: throw NoSuchElementException("Pod $pointedTo has no samples")
                sample = readSample()
                return oldSample
            }

            private fun readSample() = caller.call("iteratorNext?iteratorKey=$iteratorKey").nullableSample()

        }.asSequence()


    }

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): S = throw UnsupportedOperationException("That's not required for PodProxy")

    override fun inputs(): List<Bean<*, *>> = throw UnsupportedOperationException("That's not required for PodProxy")

    override val parameters: BeanParams
        get() = throw UnsupportedOperationException("That's not required for PodProxy")

    override fun toString(): String = "${this::class.simpleName}->$pointedTo"
}