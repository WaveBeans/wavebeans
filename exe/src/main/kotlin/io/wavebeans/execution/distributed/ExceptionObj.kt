package io.wavebeans.execution.distributed

import io.wavebeans.lib.WaveBeansClassLoader
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName

@Serializable
class ExceptionObj(
        val clazz: String,
        val message: String,
        val stackTrace: List<String>,
        val cause: ExceptionObj?
) {
    companion object {
        fun create(e: Throwable, depth: Int = 0): ExceptionObj {
            val cause = if (e.cause != null && e !== e.cause && depth < 10) create(e.cause!!, depth + 1) else null
            return ExceptionObj(
                    clazz = e::class.jvmName,
                    message = e.message ?: "",
                    stackTrace = e.stackTrace.map { "${it.className}.${it.methodName}(${it.fileName}:${it.lineNumber})" },
                    cause = cause
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun toException(): CallingException =
            CallingException(
                    WaveBeansClassLoader.classForName(clazz).kotlin as KClass<out Throwable>,
                    message,
                    stackTrace,
                    cause?.toException()
            )
}