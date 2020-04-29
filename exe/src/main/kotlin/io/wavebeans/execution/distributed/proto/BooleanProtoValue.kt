package io.wavebeans.execution.distributed.proto

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoId

fun Boolean?.toProtoValue() = BooleanProtoValue(this == null, this ?: false)

@Serializable
data class BooleanProtoValue(
        @ProtoId(1)
        override val isNull: Boolean,
        @ProtoId(2)
        override val value: Boolean
) : ProtoValue<Boolean>

