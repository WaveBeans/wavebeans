package io.wavebeans.execution.distributed

import io.wavebeans.execution.distributed.proto.ListProtoValue
import io.wavebeans.execution.distributed.proto.ProtoValue
import io.wavebeans.execution.distributed.proto.toProtoValue
import io.wavebeans.lib.WaveBeansClassLoader
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoId
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName

@Serializable
data class ExceptionDescriptor(
        @ProtoId(1)
        val clazz: String,
        @ProtoId(2)
        val message: String,
        @ProtoId(3)
        val stackTrace: List<String> = emptyList()
) {
    companion object {
        val protoEmpty = ExceptionDescriptor("0", "0", emptyList())

        fun create(e: Throwable): ExceptionDescriptor =
                ExceptionDescriptor(
                        clazz = e::class.jvmName,
                        message = e.message ?: "",
                        stackTrace = e.stackTrace.map { "${it.className}.${it.methodName}(${it.fileName}:${it.lineNumber})" }
                )
    }
}

@Serializable
class ExceptionObj(
        @ProtoId(1)
        val descriptor: ExceptionDescriptor,
        @ProtoId(2)
        val causes: ListProtoValue<ExceptionDescriptor>
) {
    companion object {

        val protoEmpty = ExceptionObj(ExceptionDescriptor.protoEmpty, (null as List<ExceptionDescriptor>?).toProtoValue())

        fun create(e: Throwable): ExceptionObj {
            var cause = e.cause
            var depth = 0
            val causes = mutableListOf<ExceptionDescriptor>()
            while (cause != null && cause !== e && depth < 10) {
                causes += ExceptionDescriptor.create(cause)
                cause = cause.cause
                depth++
            }
            return ExceptionObj(
                    descriptor = ExceptionDescriptor.create(e),
                    causes = causes.toProtoValue()
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun toException(): CallingException {
        val c = (causes.fromProtoValue() ?: emptyList()).reversed().iterator()
        var cause: CallingException? = null
        while (c.hasNext()) {
            val e = c.next()
            cause = CallingException(
                    WaveBeansClassLoader.classForName(e.clazz).kotlin as KClass<out Throwable>,
                    e.message,
                    e.stackTrace,
                    cause
            )
        }
        return CallingException(
                WaveBeansClassLoader.classForName(descriptor.clazz).kotlin as KClass<out Throwable>,
                descriptor.message,
                descriptor.stackTrace,
                cause
        )
    }

}

fun ExceptionObj?.toProtoValue(): ExceptionObjProtoValue =
        ExceptionObjProtoValue(this == null, this ?: ExceptionObj.protoEmpty)

@Serializable
class ExceptionObjProtoValue(
        @ProtoId(1)
        override val isNull: Boolean,
        @ProtoId(2)
        override val value: ExceptionObj
) : ProtoValue<ExceptionObj>