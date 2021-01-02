package io.wavebeans.lib.stream

import io.wavebeans.lib.BeanStream

interface FiniteToStream<T : Any> {
    fun convert(finiteStream: FiniteStream<T>): BeanStream<T>
}

fun <T : Any> FiniteStream<T>.stream(converter: FiniteToStream<T>): BeanStream<T> = converter.convert(this)
