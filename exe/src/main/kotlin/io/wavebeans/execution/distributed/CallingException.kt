package io.wavebeans.execution.distributed

import kotlin.reflect.KClass

class CallingException(
        val inherentExceptionClazz: KClass<out Throwable>,
        val inherentMessage: String,
        val inherentStackTrace: List<String>,
        override val cause: CallingException?
) : Exception(
        "Inherent exception ${inherentExceptionClazz}: ${inherentMessage}\n" +
                "Stack trace:\n" +
                inherentStackTrace.joinToString("\n", postfix = "\n----end of stackTrace----") { "\t at $it" },
        cause
)