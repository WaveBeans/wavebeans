package mux.lib.io

import mux.lib.BeanStream
import mux.lib.SourceBean
import mux.lib.Sample
import mux.lib.SampleArray
import java.util.concurrent.TimeUnit

interface FiniteInput : BeanStream<SampleArray, FiniteInput>, SourceBean<SampleArray, FiniteInput> {

    /** Amount of samples available */
    fun samplesCount(): Int

    /** The length of the input in provided time unit. */
    fun length(timeUnit: TimeUnit): Long
}