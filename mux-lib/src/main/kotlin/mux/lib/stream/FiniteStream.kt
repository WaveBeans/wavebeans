package mux.lib.stream

import mux.lib.Sample
import mux.lib.TimeRangeProjectable
import java.util.concurrent.TimeUnit

interface FiniteStream : MuxStream<Sample>, TimeRangeProjectable<FiniteStream> {

    fun length(timeUnit: TimeUnit = TimeUnit.MILLISECONDS): Long

}