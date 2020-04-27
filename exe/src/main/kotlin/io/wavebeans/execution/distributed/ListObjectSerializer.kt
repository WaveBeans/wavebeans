package io.wavebeans.execution.distributed

import io.wavebeans.lib.WaveBeansClassLoader
import kotlinx.serialization.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlin.reflect.jvm.jvmName

object ListObjectSerializer : KSerializer<List<Any>> {

    private const val emptyListType = "empty"

    override val descriptor: SerialDescriptor = SerialDescriptor(ListObjectSerializer::class.jvmName) {
        element("elementType", String.serializer().descriptor)
        element("elements", ListSerializer(AnySerializer()).descriptor)
    }

    override fun deserialize(decoder: Decoder): List<Any> {
        val dec = decoder.beginStructure(descriptor)
        var typeRef: String? = null
        var list: List<Any>? = null
        @Suppress("UNCHECKED_CAST")
        loop@ while (true) {
            when (val i = dec.decodeElementIndex(descriptor)) {
                CompositeDecoder.READ_DONE -> break@loop
                0 -> typeRef = dec.decodeStringElement(descriptor, i)
                1 -> list = if (typeRef != emptyListType) {
                    val type = WaveBeansClassLoader.classForName(typeRef!!).kotlin
                    dec.decodeSerializableElement(descriptor, i, ListSerializer(AnySerializer(type)))
                } else {
                    dec.decodeSerializableElement(descriptor, i, ListSerializer(AnySerializer()))
                    emptyList()
                }
                else -> throw SerializationException("Unknown index $i")
            }
        }
        dec.endStructure(descriptor)
        return list!!
    }

    override fun serialize(encoder: Encoder, value: List<Any>) {
        val elType = if (value.isNotEmpty()) value.first()::class.jvmName else emptyListType
        val s = encoder.beginStructure(descriptor)
        s.encodeStringElement(descriptor, 0, elType)
        s.encodeSerializableElement(descriptor, 1, ListSerializer(AnySerializer()), value)
        s.endStructure(descriptor)
    }
}