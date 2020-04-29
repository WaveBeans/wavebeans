package io.wavebeans.execution.distributed.proto

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoId

fun IntArray?.toProtoValue(): IntArrayProtoValue = IntArrayProtoValue(this == null, this ?: IntArray(0))

@Serializable
data class IntArrayProtoValue(
        @ProtoId(1)
        override val isNull: Boolean,
        @ProtoId(2)
        override val value: IntArray
) : ProtoValue<IntArray> {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as IntArrayProtoValue

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