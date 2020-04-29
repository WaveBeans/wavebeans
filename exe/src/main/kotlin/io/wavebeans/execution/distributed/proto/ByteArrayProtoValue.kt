package io.wavebeans.execution.distributed.proto

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoId

fun ByteArray?.toProtoValue(): ByteArrayProtoValue = ByteArrayProtoValue(this == null, this ?: ByteArray(0))

@Serializable
data class ByteArrayProtoValue(
        @ProtoId(1)
        override val isNull: Boolean,
        @ProtoId(2)
        override val value: ByteArray
) : ProtoValue<ByteArray> {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ByteArrayProtoValue

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