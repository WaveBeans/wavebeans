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

    override fun extractElement(at: Int): Any? {
        return if (at < items.size) items[at] else null
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SerializableMedium

        if (items != other.items) return false

        return true
    }

    override fun hashCode(): Int {
        return items.hashCode()
    }
}

object SerializableMediumSerializer : KSerializer<SerializableMedium> {
    override val descriptor: SerialDescriptor = SerialDescriptor(SerializableMedium::class.jvmName) {
        element("items", ListObjectSerializer.descriptor)
    }

    override fun deserialize(decoder: Decoder): SerializableMedium {
        val dec = decoder.beginStructure(descriptor)
        var l: List<Any>? = null
        loop@ while (true) {
            when (val i = dec.decodeElementIndex(descriptor)) {
                CompositeDecoder.READ_DONE -> break@loop
                0 -> l = dec.decodeSerializableElement(descriptor, i, ListObjectSerializer)
                else -> throw SerializationException("Unknown index $i")
            }
        }

        dec.endStructure(descriptor)
        return SerializableMedium(l!!)
    }

    override fun serialize(encoder: Encoder, value: SerializableMedium) {
        val s = encoder.beginStructure(descriptor)
        s.encodeSerializableElement(descriptor, 0, ListObjectSerializer, value.items)
        s.endStructure(descriptor)
    }

}