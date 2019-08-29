package mux.lib.io

import mux.lib.Sample
import mux.lib.TimeRangeProjectable
import mux.lib.stream.MuxStream
import java.util.concurrent.TimeUnit

interface FiniteInput: MuxStream<Sample>, TimeRangeProjectable<FiniteInput> {

    /** Amount of samples available */
    fun samplesCount(): Int

    /** The length of the input in provided time unit. */
    fun length(timeUnit: TimeUnit): Long

}