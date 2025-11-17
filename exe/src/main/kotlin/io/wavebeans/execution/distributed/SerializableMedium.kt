package io.wavebeans.execution.distributed

import io.wavebeans.execution.medium.Medium
import io.wavebeans.execution.medium.MediumBuilder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.reflect.jvm.jvmName

class SerializableMediumBuilder : MediumBuilder {
    override fun from(objects: List<Any>): Medium = SerializableMedium(objects)
}

/**
 * [Medium] for distributed execution.
 */
@Serializable//(with = SerializableMediumSerializer::class)
class SerializableMedium(
    @Serializable(with = ListObjectSerializer::class)
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