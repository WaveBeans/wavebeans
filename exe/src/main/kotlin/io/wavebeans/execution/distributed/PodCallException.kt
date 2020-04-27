package io.wavebeans.execution.distributed

import kotlin.reflect.KClass

class PodCallException(
        val clazz: KClass<out Throwable>,
        override val message: String,
        val stackTrace: List<String>,
        override val cause: PodCallException?
) : Exception("${clazz}: ${message}\nStackTrace:\n${stackTrace.joinToString("\n")}", cause)