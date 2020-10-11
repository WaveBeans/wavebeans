package io.wavebeans.execution.distributed.proto

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

fun <T : Any> List<T>?.toProtoValue(): ListProtoValue<T> = ListProtoValue(this == null, this ?: emptyList())

@Serializable
data class ListProtoValue<T>(
        @ProtoNumber(1)
        override val isNull: Boolean,
        @ProtoNumber(2)
        override val value: List<T> = emptyList()
) : ProtoValue<List<T>>