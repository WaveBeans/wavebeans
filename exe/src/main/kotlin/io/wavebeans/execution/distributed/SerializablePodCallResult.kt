package io.wavebeans.execution.distributed

import io.wavebeans.execution.Call
import io.wavebeans.execution.medium.PodCallResult
import io.wavebeans.execution.medium.PodCallResultBuilder
import io.wavebeans.lib.WaveBeansClassLoader
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import java.io.InputStream
import java.io.OutputStream
import kotlin.reflect.full.createInstance
import kotlin.reflect.jvm.jvmName

internal const val nullType = "null"

class SerializablePodCallResultBuilder : PodCallResultBuilder {

    override fun ok(call: Call, value: Any?): PodCallResult {
        return SerializablePodCallResult(value, call, null)
    }

    override fun error(call: Call, exception: Throwable): PodCallResult {
        return SerializablePodCallResult(null, call, exception)
    }

    override fun fromInputStream(toInputStream: InputStream): PodCallResult {
        val bytes = toInputStream.readBytes()
        val container = bytes.toObj(SerializablePodCallResultContainer.serializer())
        val serializerClazzRef = container.objSerializerRef
        val obj = if (serializerClazzRef != nullType) {
            val cl = WaveBeansClassLoader.classForName(serializerClazzRef).kotlin
            val serializer = (cl.objectInstance ?: cl.createInstance()) as KSerializer<*>
            container.objBuffer?.toObj(serializer)
        } else {
            null
        }
        return SerializablePodCallResult(
                obj,
                container.call,
                container.exception?.toException()
        )
    }
}

class SerializablePodCallResult(
        override val obj: Any?,
        override val call: Call,
        override val exception: Throwable?
) : PodCallResult {

    override fun writeTo(outputStream: OutputStream) {
        val (serializer, buf) = if (obj != null) {
            val s = SerializableRegistry.find(obj::class)
            Pair(s::class.jvmName, obj.toByteArray(s))
        } else {
            Pair(nullType, byteArrayOf())
        }

        val containerBuf = SerializablePodCallResultContainer(
                call,
                serializer,
                buf,
                exception?.let { ExceptionObj.create(it) }
        ).toByteArray(SerializablePodCallResultContainer.serializer())

        outputStream.write(containerBuf)
        outputStream.flush()
    }

    override fun toString(): String {
        return "SerializablePodCallResult(obj=$obj, call=$call, exception=$exception)"
    }
}

@Serializable
internal class SerializablePodCallResultContainer(
        val call: Call,
        val objSerializerRef: String,
        val objBuffer: ByteArray?,
        val exception: ExceptionObj?
)