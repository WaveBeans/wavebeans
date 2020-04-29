package io.wavebeans.execution.distributed.proto

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoId

fun FloatArray?.toProtoValue(): FloatArrayProtoValue = FloatArrayProtoValue(this == null, this ?: FloatArray(0))

@Serializable
data class FloatArrayProtoValue(
        @ProtoId(1)
        override val isNull: Boolean,
        @ProtoId(2)
        override val value: FloatArray
) : ProtoValue<FloatArray> {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FloatArrayProtoValue

        if (isNull != other.isNull) return false
        if (!value.contentEquals(other.value)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isNull.hashCode()
        result = 31 * result + value.contentHashCode()
        return result
    }
}