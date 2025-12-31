package io.wavebeans.execution.distributed

import io.wavebeans.lib.WaveBeansClassLoader
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlin.reflect.jvm.jvmName

object ListObjectSerializer : KSerializer<List<Any>> {

    private const val emptyListType = "empty"

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(ListObjectSerializer::class.jvmName) {
        element("elementType", String.serializer().descriptor)
        element("elements", ListSerializer(AnySerializer()).descriptor)
    }

    override fun deserialize(decoder: Decoder): List<Any> {
        return decoder.decodeStructure(descriptor) {
            lateinit var typeRef: String
            lateinit var list: List<Any>
            @Suppress("UNCHECKED_CAST")
            loop@ while (true) {
                when (val i = decodeElementIndex(descriptor)) {
                    CompositeDecoder.DECODE_DONE -> break@loop
                    0 -> typeRef = decodeStringElement(descriptor, i)
                    1 -> list = if (typeRef != emptyListType) {
                        val type = WaveBeansClassLoader.classForName(typeRef)
                        decodeSerializableElement(descriptor, i, ListSerializer(AnySerializer(type)))
                    } else {
                        decodeSerializableElement(descriptor, i, ListSerializer(AnySerializer()))
                        emptyList()
                    }

                    else -> throw SerializationException("Unknown index $i")
                }
            }
            list
        }
    }

    override fun serialize(encoder: Encoder, value: List<Any>) {
        val elType = if (value.isNotEmpty()) value.first()::class.jvmName else emptyListType
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, elType)
            encodeSerializableElement(descriptor, 1, ListSerializer(AnySerializer()), value)
        }
    }
}