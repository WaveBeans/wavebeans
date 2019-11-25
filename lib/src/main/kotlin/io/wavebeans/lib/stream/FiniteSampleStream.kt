package io.wavebeans.lib.stream

import io.wavebeans.lib.BeanStream
import io.wavebeans.lib.SampleArray
import java.util.concurrent.TimeUnit

interface FiniteSampleStream : BeanStream<SampleArray, FiniteSampleStream> {

    fun length(timeUnit: TimeUnit = TimeUnit.MILLISECONDS): Long
}