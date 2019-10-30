package mux.lib.io

import mux.lib.SinkBean
import java.io.Closeable

interface StreamOutput<T : Any, S : Any> : SinkBean<T, S> {

    fun writer(sampleRate: Float): Writer
}

interface Writer : Closeable {

    fun write(): Boolean

}
