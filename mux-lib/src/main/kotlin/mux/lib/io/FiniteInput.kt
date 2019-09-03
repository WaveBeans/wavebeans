package mux.lib.io

import mux.lib.MuxStream
import mux.lib.Sample
import java.util.concurrent.TimeUnit

interface FiniteInput: MuxStream<Sample, FiniteInput> {

    /** Amount of samples available */
    fun samplesCount(): Int

    /** The length of the input in provided time unit. */
    fun length(timeUnit: TimeUnit): Long

}