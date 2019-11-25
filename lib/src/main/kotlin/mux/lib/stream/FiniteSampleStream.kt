package mux.lib.stream

import mux.lib.BeanStream
import mux.lib.Sample
import mux.lib.SampleArray
import java.util.concurrent.TimeUnit

interface FiniteSampleStream : BeanStream<SampleArray, FiniteSampleStream> {

    fun length(timeUnit: TimeUnit = TimeUnit.MILLISECONDS): Long
}