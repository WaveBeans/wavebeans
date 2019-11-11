package mux.lib.execution

import mux.lib.Bean
import mux.lib.BeanParams
import mux.lib.BeanStream
import java.util.concurrent.TimeUnit

abstract class MergingPodProxy<T : Any, S : Any>(
        val converter: (PodCallResult) -> List<T>?
) : BeanStream<T, S>, PodProxy<T, S> {

    abstract val readsFrom: List<PodKey>

    override fun asSequence(sampleRate: Float): Sequence<T> {
        return object : Iterator<T> {

            val iterators = readsFrom.map { PodProxyIterator(sampleRate, it, converter = converter) }.toTypedArray()
            var iteratorToReadFrom = 0

            override fun hasNext(): Boolean {
                return iterators[iteratorToReadFrom].hasNext()
            }

            override fun next(): T {
                val el = iterators[iteratorToReadFrom].next()
                iteratorToReadFrom = (iteratorToReadFrom + 1) % iterators.size
                return el
            }

        }.asSequence()
    }

    override fun rangeProjection(start: Long, end: Long?, timeUnit: TimeUnit): S {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun inputs(): List<Bean<*, *>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override val parameters: BeanParams
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override fun toString(): String {
        return "${this::class.simpleName}->$readsFrom"
    }
}