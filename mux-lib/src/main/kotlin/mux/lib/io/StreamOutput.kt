package mux.lib.io

import mux.lib.MuxNode
import mux.lib.SingleMuxNode
import java.io.Closeable
import java.util.concurrent.TimeUnit

interface StreamOutput<T : Any, S : Any> : Closeable, SingleMuxNode<T, S> {

    fun writer(sampleRate: Float): Writer
}

interface Writer : Closeable {

    fun write(duration: Long, timeUnit: TimeUnit = TimeUnit.MILLISECONDS): Boolean

}
