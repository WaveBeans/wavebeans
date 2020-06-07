package io.wavebeans.execution.medium

import io.wavebeans.execution.Call
import io.wavebeans.execution.config.ExecutionConfig
import java.io.InputStream
import java.io.OutputStream
import kotlin.reflect.KClass

interface PodCallResultBuilder {
    fun ok(call: Call, value: Any?): PodCallResult

    fun error(call: Call, exception: Throwable): PodCallResult

    fun fromInputStream(toInputStream: InputStream): PodCallResult
}

interface PodCallResult {

    companion object {
        fun ok(call: Call, value: Any?): PodCallResult = ExecutionConfig.podCallResultBuilder().ok(call, value)

        fun error(call: Call, exception: Throwable): PodCallResult = ExecutionConfig.podCallResultBuilder().error(call, exception)
    }

    val obj: Any?

    val call: Call

    val exception: Throwable?

    fun isNull(): Boolean = obj == null

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> value(clazz: KClass<T>): T? = if (exception != null) throw exception!! else obj as T?

    fun stream(): InputStream
}

inline fun <reified T : Any> PodCallResult.valueOrNull(): T? = this.value(T::class)

inline fun <reified T : Any> PodCallResult.value(): T = this.value(T::class)
        ?: throw IllegalStateException("The value of [$this] requested as not-null but it is null")