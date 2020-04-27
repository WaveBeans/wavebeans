package io.wavebeans.execution.distributed

import io.wavebeans.execution.medium.Medium
import io.wavebeans.execution.medium.MediumBuilder
import io.wavebeans.execution.medium.MediumSerializer
import kotlinx.serialization.*
import kotlin.reflect.jvm.jvmName

class SerializableMediumBuilder : MediumBuilder {
    override fun from(objects: List<Any>): Medium = SerializableMedium(objects)
}

/**
 * [Medium] for distributed execution.
 */
@Serializable(with = SerializableMediumSerializer::class)
class SerializableMedium(
        val items: List<Any>
) : Medium {

    override fun serializer(): MediumSerializer = TODO()

    override fun extractElement(at: Int): Any? {
        return if (at < items.size) items[at] else null
    }
}

object SerializableMediumSerializer : KSerializer<SerializableMedium> {
    override val descriptor: SerialDescriptor = SerialDescriptor(SerializableMedium::class.jvmName) {
        element("items", ListObjectSerializer.descriptor)
    }

    override fun deserialize(decoder: Decoder): SerializableMedium {
        val s = decoder.beginStructure(descriptor)
        val l = s.decodeSerializableElement(descriptor, 0, ListObjectSerializer)
        s.endStructure(descriptor)
        return SerializableMedium(l)
    }

    override fun serialize(encoder: Encoder, value: SerializableMedium) {
        val s = encoder.beginStructure(descriptor)
        s.encodeSerializableElement(descriptor, 0, ListObjectSerializer, value.items)
        s.endStructure(descriptor)
    }

}