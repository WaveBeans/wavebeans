package mux.lib.io

import mux.lib.MuxNode
import mux.lib.MuxStream
import java.io.*
import java.util.concurrent.TimeUnit

interface StreamOutput : Closeable {

    fun write(sampleRate: Float, start: Long? = null, end: Long? = null, timeUnit: TimeUnit = TimeUnit.MILLISECONDS): Boolean

    fun mux(): MuxNode

}

