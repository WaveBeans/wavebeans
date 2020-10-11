package io.wavebeans.execution.distributed.proto

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

fun Float?.toProtoValue() = FloatProtoValue(this == null, this ?: Float.MIN_VALUE)

@Serializable
data class FloatProtoValue(
        @ProtoNumber(1)
        override val isNull: Boolean,
        @ProtoNumber(2)
        override val value: Float
) : ProtoValue<Float>

