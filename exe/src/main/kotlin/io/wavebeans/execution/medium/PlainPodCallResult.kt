package io.wavebeans.execution.medium

import io.wavebeans.execution.Call
import java.io.InputStream
import java.io.OutputStream
import kotlin.reflect.KClass

class PlainPodCallResultBuilder : PodCallResultBuilder {
    override fun ok(call: Call, value: Any?): PodCallResult = PlainPodCallResult(call, null, value)

    override fun error(call: Call, exception: Throwable): PodCallResult = PlainPodCallResult(call, exception, null)

    override fun fromInputStream(toInputStream: InputStream): PodCallResult =
            throw UnsupportedOperationException("It doesn't support creating from stream")
}

class PlainPodCallResult(
        override val call: Call,
        override val exception: Throwable?,
        override val obj: Any?
) : PodCallResult {

    override fun stream(): InputStream = throw UnsupportedOperationException("It doesn't support writing to stream")

    override fun toString(): String {
        return "PlainPodCallResult(call=$call, exception=$exception, obj=$obj)"
    }
}