package io.wavebeans.execution.medium

import io.wavebeans.execution.Call
import kotlin.reflect.KClass

class PlainPodCallResultBuilder : PodCallResultBuilder {
    override fun ok(call: Call, value: Any?): PodCallResult = PlainPodCallResult(call, null, value)

    override fun error(call: Call, exception: Throwable): PodCallResult = PlainPodCallResult(call, exception, null)
}

class PlainPodCallResult(
        override val call: Call,
        override val exception: Throwable?,
        val any: Any?
) : PodCallResult {

    override fun isNull(): Boolean = any == null

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> value(clazz: KClass<T>): T? = any as T
}