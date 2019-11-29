package io.wavebeans.lib.io

import io.wavebeans.lib.BeanStream
import io.wavebeans.lib.Sample
import io.wavebeans.lib.SourceBean
import java.util.concurrent.TimeUnit

interface FiniteInput : BeanStream<Sample, FiniteInput>, SourceBean<Sample, FiniteInput> {

    /** Amount of samples available */
    fun samplesCount(): Int

    /** The length of the input in provided time unit. */
    fun length(timeUnit: TimeUnit): Long
}