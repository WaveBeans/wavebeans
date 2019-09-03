package mux.lib.io

import java.io.*
import java.util.concurrent.TimeUnit

interface StreamOutput : Closeable {

    fun write(sampleRate: Float, start: Long? = null, end: Long? = null, timeUnit: TimeUnit = TimeUnit.MILLISECONDS)

}

