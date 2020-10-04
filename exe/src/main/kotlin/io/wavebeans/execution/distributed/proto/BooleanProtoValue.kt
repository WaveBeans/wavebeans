package io.wavebeans.execution.distributed.proto

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

fun Boolean?.toProtoValue() = BooleanProtoValue(this == null, this ?: false)

@Serializable
data class BooleanProtoValue(
        @ProtoNumber(1)
        override val isNull: Boolean,
        @ProtoNumber(2)
        override val value: Boolean
) : ProtoValue<Boolean>

