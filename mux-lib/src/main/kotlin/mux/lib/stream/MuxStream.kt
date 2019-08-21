package mux.lib.stream

interface MuxStream<T> {
    fun asSequence(sampleRate: Float): Sequence<T>
}