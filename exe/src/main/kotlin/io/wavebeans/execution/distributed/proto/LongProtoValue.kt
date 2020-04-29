package io.wavebeans.execution.distributed.proto

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoId

fun Long?.toProtoValue() = LongProtoValue(this == null, this ?: Long.MIN_VALUE)

@Serializable
data class LongProtoValue(
        @ProtoId(1)
        override val isNull: Boolean,
        @ProtoId(2)
        override val value: Long
) : ProtoValue<Long>

