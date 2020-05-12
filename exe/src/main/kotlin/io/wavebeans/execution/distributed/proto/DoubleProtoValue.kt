package io.wavebeans.execution.distributed.proto

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoId

fun Double?.toProtoValue() = DoubleProtoValue(this == null, this ?: Double.MIN_VALUE)

@Serializable
data class DoubleProtoValue(
        @ProtoId(1)
        override val isNull: Boolean,
        @ProtoId(2)
        override val value: Double
) : ProtoValue<Double>

