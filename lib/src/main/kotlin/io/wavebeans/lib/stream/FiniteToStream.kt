package io.wavebeans.lib.stream

import io.wavebeans.lib.BeanStream
import io.wavebeans.lib.Sample

fun <T : Any> FiniteStream<T>.stream(converter: FiniteToStream<T>): BeanStream<T> = converter.convert(this)

interface FiniteToStream<T : Any> {

    fun convert(finiteStream: FiniteStream<T>): BeanStream<T>
}
