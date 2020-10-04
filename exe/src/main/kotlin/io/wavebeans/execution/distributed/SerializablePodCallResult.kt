package io.wavebeans.execution.distributed

import io.wavebeans.communicator.ExceptionObj
import io.wavebeans.execution.Call
import io.wavebeans.execution.distributed.proto.ByteArrayProtoValue
import io.wavebeans.execution.distributed.proto.ProtoObj
import io.wavebeans.execution.distributed.proto.ProtoValue
import io.wavebeans.execution.distributed.proto.toProtoValue
import io.wavebeans.execution.medium.PodCallResult
import io.wavebeans.execution.medium.PodCallResultBuilder
import io.wavebeans.lib.WaveBeansClassLoader
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import java.io.ByteArrayInputStream
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
        val container = bytes.asObj(SerializablePodCallResultContainer.serializer())
        val serializerClazzRef = container.objSerializerRef
        val obj = if (serializerClazzRef != nullType) {
            val cl = WaveBeansClassLoader.classForName(serializerClazzRef).kotlin
            val serializer = (cl.objectInstance ?: cl.createInstance()) as KSerializer<*>
            container.objBuffer.fromProtoValue()?.asObj(serializer)
        } else {
            null
        }
        val newObj = ProtoObj.unwrapIfNeeded(obj)
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

    override fun stream(): InputStream {
        val (serializer, buf) = if (obj != null) {
            val newObj = ProtoObj.wrapIfNeeded(obj)
            val s = SerializableRegistry.find(newObj::class)
            Pair(s::class.jvmName, newObj.asByteArray(s))
        } else {
            Pair(nullType, ByteArray(0))
        }

        val containerBuf = SerializablePodCallResultContainer(
                call,
                serializer,
                buf.toProtoValue(),
                exception?.toExceptionObj().toProtoValue()
        ).asByteArray(SerializablePodCallResultContainer.serializer())

        return ByteArrayInputStream(containerBuf)
    }

    override fun toString(): String {
        return "SerializablePodCallResult(obj=$obj, call=$call, exception=$exception)"
    }
}

@Serializable
internal class SerializablePodCallResultContainer(
        @ProtoNumber(1)
        val call: Call,
        @ProtoNumber(2)
        val objSerializerRef: String,
        @ProtoNumber(3)
        val objBuffer: ByteArrayProtoValue,
        @ProtoNumber(4)
        val exception: ExceptionObjProtoValue
)