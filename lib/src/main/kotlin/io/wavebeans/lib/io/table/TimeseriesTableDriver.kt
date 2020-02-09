package io.wavebeans.lib.io.table

import io.wavebeans.lib.*
import java.io.Closeable

interface TimeseriesTableDriver<T : Any> : Closeable {

    fun put(time: TimeMeasure, value: T)

    fun last(interval: TimeMeasure): BeanStream<T>

    fun timeRange(from: TimeMeasure, to: TimeMeasure): BeanStream<T>
}

