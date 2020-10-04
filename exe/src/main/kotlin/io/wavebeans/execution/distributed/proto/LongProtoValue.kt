package io.wavebeans.execution.distributed.proto

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

fun Long?.toProtoValue() = LongProtoValue(this == null, this ?: Long.MIN_VALUE)

@Serializable
data class LongProtoValue(
        @ProtoNumber(1)
        override val isNull: Boolean,
        @ProtoNumber(2)
        override val value: Long
) : ProtoValue<Long>

