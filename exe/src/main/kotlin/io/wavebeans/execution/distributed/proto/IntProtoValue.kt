package io.wavebeans.execution.distributed.proto

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

fun Int?.toProtoValue() = IntProtoValue(this == null, this ?: Int.MIN_VALUE)

@Serializable
data class IntProtoValue(
        @ProtoNumber(1)
        override val isNull: Boolean,
        @ProtoNumber(2)
        override val value: Int
) : ProtoValue<Int>

