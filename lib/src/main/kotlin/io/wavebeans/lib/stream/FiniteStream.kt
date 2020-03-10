package io.wavebeans.lib.stream

import io.wavebeans.lib.BeanStream
import io.wavebeans.lib.Sample
import java.util.concurrent.TimeUnit

interface FiniteStream<T : Any> : BeanStream<T> {

    fun length(timeUnit: TimeUnit = TimeUnit.MILLISECONDS): Long
}
