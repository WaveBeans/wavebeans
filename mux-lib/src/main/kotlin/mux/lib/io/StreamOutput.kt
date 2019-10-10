package mux.lib.io

import mux.lib.SingleBean
import java.io.Closeable

interface StreamOutput<T : Any, S : Any> : SingleBean<T, S> {

    fun writer(sampleRate: Float): Writer
}

interface Writer : Closeable {

    fun write(): Boolean

}
