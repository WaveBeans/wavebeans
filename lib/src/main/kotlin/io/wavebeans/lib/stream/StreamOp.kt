package io.wavebeans.lib.stream

interface StreamOp<T : Any> {
    fun apply(a: T, b: T): T
}

interface ScalarOp<C : Number, T : Any> {
    fun apply(a: T, b: C): T
}