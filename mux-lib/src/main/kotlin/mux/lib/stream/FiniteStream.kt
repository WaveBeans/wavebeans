package mux.lib.stream

import mux.lib.MuxStream
import mux.lib.Sample
import java.util.concurrent.TimeUnit

interface FiniteStream : MuxStream<Sample, FiniteStream> {

    fun length(timeUnit: TimeUnit = TimeUnit.MILLISECONDS): Long

}