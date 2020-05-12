package io.wavebeans.execution.distributed.proto

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoId

fun Float?.toProtoValue() = FloatProtoValue(this == null, this ?: Float.MIN_VALUE)

@Serializable
data class FloatProtoValue(
        @ProtoId(1)
        override val isNull: Boolean,
        @ProtoId(2)
        override val value: Float
) : ProtoValue<Float>

