package mux.lib.stream

import mux.lib.BeanStream
import java.util.concurrent.TimeUnit

interface FiniteFftStream : BeanStream<FftSample, FiniteFftStream> {

    fun length(timeUnit: TimeUnit = TimeUnit.MILLISECONDS): Long
}

