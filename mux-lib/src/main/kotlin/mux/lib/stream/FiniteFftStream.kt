package mux.lib.stream

import mux.lib.TimeRangeProjectable
import java.util.concurrent.TimeUnit

interface FiniteFftStream : MuxStream<FftSample>, TimeRangeProjectable<FiniteFftStream> {

    fun length(timeUnit: TimeUnit = TimeUnit.MILLISECONDS): Long

}

