package io.wavebeans.lib

import kotlinx.serialization.Serializable

fun executionScope(block: FnInitParameters.() -> FnInitParameters) = ExecutionScope(FnInitParameters().let(block))

@Serializable
data class ExecutionScope(val parameters: FnInitParameters)

val EmptyScope = ExecutionScope(FnInitParameters())