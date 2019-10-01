package mux.lib.stream

import mux.lib.BeanStream
import mux.lib.Sample
import java.util.concurrent.TimeUnit

interface FiniteSampleStream : BeanStream<Sample, FiniteSampleStream> {

    fun length(timeUnit: TimeUnit = TimeUnit.MILLISECONDS): Long
}