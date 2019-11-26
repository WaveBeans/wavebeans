package io.wavebeans.lib.stream

import io.wavebeans.lib.BeanStream
import java.util.concurrent.TimeUnit

interface FiniteFftStream : BeanStream<FftSample, FiniteFftStream> {

    fun length(timeUnit: TimeUnit = TimeUnit.MILLISECONDS): Long
}

