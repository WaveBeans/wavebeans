package io.wavebeans.execution.distributed.proto

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoId

fun <T : Any> List<T>?.toProtoValue(): ListProtoValue<T> = ListProtoValue(this == null, this ?: emptyList())

@Serializable
data class ListProtoValue<T>(
        @ProtoId(1)
        override val isNull: Boolean,
        @ProtoId(2)
        override val value: List<T> = emptyList()
) : ProtoValue<List<T>>