package io.wavebeans.lib.io

import io.wavebeans.lib.SinkBean

/**
 * The type of [SinkBean] that outputs the stream somewhere.
 */
interface StreamOutput<T : Any> : SinkBean<T> {

    /**
     * Gets the [Writer] to perform iterative writes with specified [sampleRate].
     */
    fun writer(sampleRate: Float): Writer
}

/**
 * The writer created by [StreamOutput] that performs iterative writes.
 */
interface Writer : Closeable {

    /**
     * Makes one iteration of write. May write in temporary buffer, always call [close] to flush the buffers.
     *
     * @return `true` if there is something else to read, else `false` if EOF is reached.
     */
    fun write(): Boolean

}
