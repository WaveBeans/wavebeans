package io.wavebeans.execution.distributed.proto

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoId

fun Int?.toProtoValue() = IntProtoValue(this == null, this ?: Int.MIN_VALUE)

@Serializable
data class IntProtoValue(
        @ProtoId(1)
        override val isNull: Boolean,
        @ProtoId(2)
        override val value: Int
) : ProtoValue<Int>

