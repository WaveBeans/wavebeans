package io.wavebeans.http

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * The objects are read from the sequence as `Any`, need to detect serializer based
 * on actual value but not the compile-time type as it is done via regular API.
 */
object PlainObjectSerializer : KSerializer<Any> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Any") {}

    override fun deserialize(decoder: Decoder): Any {
        throw IllegalStateException("This serializer can only be used for serialization!")
    }

    override fun serialize(encoder: Encoder, value: Any) {
        val s = JsonBeanStreamReader.find(value::class) ?: serializer(value::class.java)
        encoder.encodeSerializableValue(s, value)
    }
}