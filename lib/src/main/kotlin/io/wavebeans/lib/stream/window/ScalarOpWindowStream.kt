package io.wavebeans.lib.stream.window

import io.wavebeans.lib.SingleBean
import io.wavebeans.lib.stream.ScalarOp

abstract class ScalarOpWindowStream<C : Number, T : Any>(
        val sourceStream: WindowStream<T>,
        val fn: ScalarOp<C, T>,
        val scalar: C
) : WindowStream<T>, SingleBean<Window<T>> {

    override fun asSequence(sampleRate: Float): Sequence<Window<T>> {
        val sourceIterator = sourceStream.asSequence(sampleRate).iterator()
        return object : Iterator<Window<T>> {

            override fun hasNext(): Boolean {
                return sourceIterator.hasNext()
            }

            override fun next(): Window<T> {
                check(sourceIterator.hasNext()) { "No more elements in the stream" }

                return Window(sourceIterator.next().elements.map { fn.apply(it, scalar) })
            }

        }.asSequence()
    }
}

