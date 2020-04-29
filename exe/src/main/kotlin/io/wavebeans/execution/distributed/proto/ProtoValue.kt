package io.wavebeans.execution.distributed.proto

interface ProtoValue<T : Any> {
    val isNull: Boolean

    val value: T

    fun fromProtoValue(): T? = if (isNull) null else value
}