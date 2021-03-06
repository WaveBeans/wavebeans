package io.wavebeans.execution.distributed

import io.wavebeans.execution.medium.Medium
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.reflect.KClass

/**
 * The objects are read from the sequence as `Any`, need to detect serializer based
 * on actual value but not the compile-time type as it is done via regular API.
 */
class AnySerializer(private val clazz: KClass<*>? = null) : KSerializer<Any> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Any") {}

    override fun deserialize(decoder: Decoder): Any {
        val s = SerializableRegistry.find(
                clazz ?: throw IllegalArgumentException("clazz must be specified for deserialization")
        )
        return decoder.decodeSerializableValue(s)
    }

    override fun serialize(encoder: Encoder, value: Any) {
        val s = SerializableRegistry.find(value::class)
        encoder.encodeSerializableValue(s, value)
    }
}