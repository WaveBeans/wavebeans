package io.wavebeans.lib.stream.fft

import io.wavebeans.lib.BeanStream
import io.wavebeans.lib.stream.fft.FftSample
import java.util.concurrent.TimeUnit

interface FiniteFftStream : BeanStream<FftSample, FiniteFftStream> {

    fun length(timeUnit: TimeUnit = TimeUnit.MILLISECONDS): Long
}

