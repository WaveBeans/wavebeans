package io.wavebeans.execution.distributed.proto

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

fun Double?.toProtoValue() = DoubleProtoValue(this == null, this ?: Double.MIN_VALUE)

@Serializable
data class DoubleProtoValue(
        @ProtoNumber(1)
        override val isNull: Boolean,
        @ProtoNumber(2)
        override val value: Double
) : ProtoValue<Double>

