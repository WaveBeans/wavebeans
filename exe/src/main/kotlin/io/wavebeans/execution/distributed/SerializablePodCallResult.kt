package io.wavebeans.execution.distributed

import io.wavebeans.execution.Call
import io.wavebeans.execution.distributed.proto.ByteArrayProtoValue
import io.wavebeans.execution.distributed.proto.ProtoValue
import io.wavebeans.execution.distributed.proto.toProtoValue
import io.wavebeans.execution.medium.PodCallResult
import io.wavebeans.execution.medium.PodCallResultBuilder
import io.wavebeans.lib.WaveBeansClassLoader
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoId
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
            container.objBuffer.fromProtoValue()?.toObj(serializer)
        } else {
            null
        }
        val newObj = if (obj is ProtoValue<*>) obj.fromProtoValue() else obj
        return SerializablePodCallResult(
                newObj,
                container.call,
                container.exception.fromProtoValue()?.toException()
        )
    }
}

class SerializablePodCallResult(
        override val obj: Any?,
        override val call: Call,
        override val exception: Throwable?
) : PodCallResult {

    override fun writeTo(outputStream: OutputStream) {
        val newObj = when (obj) {
            is Long? -> obj.toProtoValue()
            is Double? -> obj.toProtoValue()
            is Float? -> obj.toProtoValue()
            is Int? -> obj.toProtoValue()
            is Boolean? -> obj.toProtoValue()
            is ByteArray? -> obj.toProtoValue()
            is DoubleArray? -> obj.toProtoValue()
            is FloatArray? -> obj.toProtoValue()
            is IntArray? -> obj.toProtoValue()
            is LongArray? -> obj.toProtoValue()
            else -> obj
        }
        val (serializer, buf) = if (newObj != null) {
            val s = SerializableRegistry.find(newObj::class)
            Pair(s::class.jvmName, newObj.toByteArray(s))
        } else {
            Pair(nullType, byteArrayOf())
        }

        val containerBuf = SerializablePodCallResultContainer(
                call,
                serializer,
                buf.toProtoValue(),
                exception?.let { ExceptionObj.create(it) }.toProtoValue()
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
        @ProtoId(1)
        val call: Call,
        @ProtoId(2)
        val objSerializerRef: String,
        @ProtoId(3)
        val objBuffer: ByteArrayProtoValue,
        @ProtoId(4)
        val exception: ExceptionObjProtoValue
)