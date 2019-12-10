package io.wavebeans.lib.stream

import io.wavebeans.lib.BeanStream
import io.wavebeans.lib.Sample
import java.util.concurrent.TimeUnit

interface FiniteSampleStream : BeanStream<Sample> {

    fun length(timeUnit: TimeUnit = TimeUnit.MILLISECONDS): Long
}
