package io.wavebeans.execution.medium

import io.wavebeans.execution.Call
import io.wavebeans.execution.config.ExecutionConfig
import kotlin.reflect.KClass

interface PodCallResultBuilder {
    fun ok(call: Call, value: Any?): PodCallResult

    fun error(call: Call, exception: Throwable): PodCallResult
}

interface PodCallResult {

    companion object {
        fun ok(call: Call, value: Any?): PodCallResult = ExecutionConfig.podCallResultBuilder().ok(call, value)

        fun error(call: Call, exception: Throwable): PodCallResult = ExecutionConfig.podCallResultBuilder().error(call, exception)
    }

    val call: Call

    val exception: Throwable?

    fun isNull(): Boolean

    fun <T : Any> value(clazz: KClass<T>): T?
}

inline fun <reified T : Any> PodCallResult.valueOrNull(): T? = this.value(T::class)

inline fun <reified T : Any> PodCallResult.value(): T = this.value(T::class)
        ?: throw IllegalStateException("The value of [$this] requested as not-null but it is null")