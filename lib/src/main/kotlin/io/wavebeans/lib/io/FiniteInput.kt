package io.wavebeans.lib.io

import io.wavebeans.lib.BeanStream
import io.wavebeans.lib.SourceBean
import io.wavebeans.lib.SampleArray
import java.util.concurrent.TimeUnit

interface FiniteInput : BeanStream<SampleArray, FiniteInput>, SourceBean<SampleArray, FiniteInput> {

    /** Amount of samples available */
    fun samplesCount(): Int

    /** The length of the input in provided time unit. */
    fun length(timeUnit: TimeUnit): Long
}