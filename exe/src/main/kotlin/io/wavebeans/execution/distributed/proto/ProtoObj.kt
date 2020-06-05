package io.wavebeans.execution.distributed.proto

import io.wavebeans.execution.distributed.SerializableRegistry
import kotlinx.serialization.KSerializer
import kotlin.reflect.KClass

object ProtoObj {

    fun serializerForMaybeWrappedObj(clazz: KClass<*>): KSerializer<*> = when (clazz) {
        Long::class -> LongProtoValue.serializer()
        Double::class-> DoubleProtoValue.serializer()
        Float::class -> FloatProtoValue.serializer()
        Int::class -> IntProtoValue.serializer()
        Boolean::class -> BooleanProtoValue.serializer()
        ByteArray::class -> ByteArrayProtoValue.serializer()
        DoubleArray::class -> DoubleArrayProtoValue.serializer()
        FloatArray::class -> FloatArrayProtoValue.serializer()
        IntArray::class -> IntArrayProtoValue.serializer()
        LongArray::class -> LongArrayProtoValue.serializer()
        else -> SerializableRegistry.find(clazz)
    }

    fun wrapIfNeeded(obj: Any?): Any = when (obj) {
        is Long? -> obj.toProtoValue()
        is Double? -> obj.toProtoValue()
        is Float? -> obj.toProtoValue()
        is Int? -> obj.toProtoValue()
        is Boolean? -> obj.toProtoValue()
        is ByteArray? -> obj.toProtoValue()
        is DoubleArray? -> obj.toProtoValue()
        is FloatArray? -> obj.toProtoValue()
        is IntArray? -> obj.toProtoValue()
        is LongArray? -> obj.toProtoValue()
        null -> throw IllegalArgumentException("input object is null and ussupported, have to be wrapped to " +
                "io.wavebeans.execution.distributed.proto.ProtoValue manually")
        else -> obj
    }

    fun unwrapIfNeeded(obj: Any?): Any? = if (obj is ProtoValue<*>) obj.fromProtoValue() else obj

}