package mux.lib.io

import mux.lib.MuxNode
import java.io.Closeable
import java.util.concurrent.TimeUnit

interface StreamOutput : Closeable {

    fun writer(sampleRate: Float): Writer

    fun mux(): MuxNode

}

interface Writer : Closeable {

    fun write(duration: Long, timeUnit: TimeUnit = TimeUnit.MILLISECONDS): Boolean

}
