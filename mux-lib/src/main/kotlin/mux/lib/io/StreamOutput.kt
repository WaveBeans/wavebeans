package mux.lib.io

import mux.lib.SingleBean
import java.io.Closeable
import java.util.concurrent.TimeUnit

interface StreamOutput<T : Any, S : Any> : Closeable, SingleBean<T, S> {

    fun writer(sampleRate: Float): Writer
}

interface Writer : Closeable {

    fun write(duration: Long, timeUnit: TimeUnit = TimeUnit.MILLISECONDS): Boolean

}
