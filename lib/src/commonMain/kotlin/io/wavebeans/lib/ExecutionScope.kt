package io.wavebeans.lib

fun executionScope(block: FnInitParameters.() -> FnInitParameters) = ExecutionScope(FnInitParameters().let(block))

data class ExecutionScope(val parameters: FnInitParameters)

val EmptyScope = ExecutionScope(FnInitParameters())