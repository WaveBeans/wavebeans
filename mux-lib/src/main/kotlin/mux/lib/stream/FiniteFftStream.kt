package mux.lib.stream

import mux.lib.MuxStream
import java.util.concurrent.TimeUnit

interface FiniteFftStream : MuxStream<FftSample, FiniteFftStream> {

    fun length(timeUnit: TimeUnit = TimeUnit.MILLISECONDS): Long

}

