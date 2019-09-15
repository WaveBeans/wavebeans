package mux.lib.io

import mux.lib.MuxNode
import mux.lib.MuxStream
import mux.lib.Restorable
import mux.lib.SingleMuxNode
import java.io.Closeable
import java.util.concurrent.TimeUnit

interface StreamOutput<T : Any, S : Any> : Closeable, Restorable<T, S> {

    fun writer(sampleRate: Float): Writer

    fun input(): MuxNode<T, S>

}

interface Writer : Closeable {

    fun write(duration: Long, timeUnit: TimeUnit = TimeUnit.MILLISECONDS): Boolean

}
