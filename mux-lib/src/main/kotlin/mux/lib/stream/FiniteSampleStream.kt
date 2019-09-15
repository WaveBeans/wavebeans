package mux.lib.stream

import mux.lib.MuxStream
import mux.lib.Sample
import java.util.concurrent.TimeUnit

interface FiniteSampleStream : MuxStream<Sample, FiniteSampleStream> {

    fun length(timeUnit: TimeUnit = TimeUnit.MILLISECONDS): Long
}