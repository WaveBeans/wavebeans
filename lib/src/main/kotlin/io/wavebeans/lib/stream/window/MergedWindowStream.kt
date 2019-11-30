package io.wavebeans.lib.stream.window

import io.wavebeans.lib.Bean
import io.wavebeans.lib.MultiBean


interface MergedWindowStreamOperation<T : Any> {

    fun apply(a: T, b: T): T
}

abstract class MergedWindowStream<T : Any, WINDOWING_STREAM : Any, S : WindowStream<T, WINDOWING_STREAM, S>>(
        val sourceStream: WindowStream<T, WINDOWING_STREAM, S>,
        val mergeStream: WindowStream<T, WINDOWING_STREAM, S>,
        val fn: MergedWindowStreamOperation<T>
) : WindowStream<T, WINDOWING_STREAM, S>, MultiBean<Window<T>, S> {

    init {
        require(sourceStream.parameters.windowSize == mergeStream.parameters.windowSize
                && sourceStream.parameters.step == sourceStream.parameters.step
        ) { "Can't merge with stream with different window size or step" }
    }

    protected abstract val zeroEl: T

    override val inputs: List<Bean<Window<T>, S>>
        get() = listOf(sourceStream, mergeStream)

    override fun asSequence(sampleRate: Float): Sequence<Window<T>> {
        val sourceIterator = sourceStream.asSequence(sampleRate).iterator()
        val mergeIterator = mergeStream.asSequence(sampleRate).iterator()
        return object : Iterator<Window<T>> {

            override fun hasNext(): Boolean {
                return sourceIterator.hasNext() || mergeIterator.hasNext()
            }

            override fun next(): Window<T> {
                check(sourceIterator.hasNext() || mergeIterator.hasNext()) { "No more elements in the stream" }

                val windowSize = sourceStream.parameters.windowSize

                val zeroWindow = Window((0 until windowSize).map { zeroEl })

                val sourceWindow = if (sourceIterator.hasNext()) sourceIterator.next() else zeroWindow
                val mergeWindow = if (mergeIterator.hasNext()) mergeIterator.next() else zeroWindow

                return Window(
                        (0 until windowSize).map { i ->
                            val s = if (i < sourceWindow.elements.size) sourceWindow.elements[i] else zeroEl
                            val m = if (i < mergeWindow.elements.size) mergeWindow.elements[i] else zeroEl

                            fn.apply(s, m)
                        }
                )
            }

        }.asSequence()
    }
}

