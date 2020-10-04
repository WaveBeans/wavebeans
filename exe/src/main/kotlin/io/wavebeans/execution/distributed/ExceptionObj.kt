package io.wavebeans.execution.distributed

import io.wavebeans.communicator.ExceptionDescriptor
import io.wavebeans.communicator.ExceptionObj
import io.wavebeans.execution.distributed.proto.ListProtoValue
import io.wavebeans.execution.distributed.proto.ProtoValue
import io.wavebeans.execution.distributed.proto.toProtoValue
import io.wavebeans.lib.WaveBeansClassLoader
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.protobuf.ProtoNumber
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName

fun Throwable.toExceptionDescriptor(): ExceptionDescriptor {
    return ExceptionDescriptor.newBuilder()
            .setClazz(this::class.jvmName)
            .setMessage(this.message ?: "")
            .addAllStackTrace(this.stackTrace.map { "${it.className}.${it.methodName}(${it.fileName}:${it.lineNumber})" })
            .build()

}

fun Throwable.toExceptionObj(): ExceptionObj {
    var cause = this.cause
    var depth = 0
    val causes = mutableListOf<ExceptionDescriptor>()
    while (cause != null && cause !== this && depth < 10) {
        causes += cause.toExceptionDescriptor()
        cause = cause.cause
        depth++
    }
    return ExceptionObj.newBuilder()
            .setExceptionDescriptor(this.toExceptionDescriptor())
            .addAllCauses(causes)
            .build()

}

@Suppress("UNCHECKED_CAST")
fun ExceptionObj.toException(): CallingException {
    val c = (this.causesList ?: emptyList()).reversed().iterator()
    var cause: CallingException? = null
    while (c.hasNext()) {
        val e = c.next()
        cause = CallingException(
                WaveBeansClassLoader.classForName(e.clazz).kotlin as KClass<out Throwable>,
                e.message,
                e.stackTraceList,
                cause
        )
    }
    return CallingException(
            WaveBeansClassLoader.classForName(exceptionDescriptor.clazz).kotlin as KClass<out Throwable>,
            exceptionDescriptor.message,
            exceptionDescriptor.stackTraceList,
            cause
    )
}

fun ExceptionObj?.toProtoValue(): ExceptionObjProtoValue =
        ExceptionObjProtoValue(this == null, this?.toByteArray() ?: ByteArray(0))

@Serializable
class ExceptionObjProtoValue(
        @ProtoNumber(1)
        override val isNull: Boolean,
        @ProtoNumber(2)
        val exceptionBuf: ByteArray
) : ProtoValue<ExceptionObj> {

    override val value: ExceptionObj
        get() = ExceptionObj.parseFrom(exceptionBuf)

}