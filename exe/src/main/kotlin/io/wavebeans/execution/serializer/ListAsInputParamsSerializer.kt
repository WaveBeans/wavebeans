package io.wavebeans.execution.serializer

import io.wavebeans.execution.distributed.AnySerializer
import io.wavebeans.lib.WaveBeansClassLoader
import io.wavebeans.lib.className
import io.wavebeans.lib.io.ListAsInputParams
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
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName

/**
 * Serializer for [ListAsInputParams].
 */
@Suppress("UNCHECKED_CAST")
object ListAsInputParamsSerializer : KSerializer<ListAsInputParams> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(ListAsInputParams::class.className()) {
        element("elementType", String.serializer().descriptor)
        element("list", ListSerializer(AnySerializer()).descriptor)
    }

    override fun deserialize(decoder: Decoder): ListAsInputParams {
        return decoder.decodeStructure(descriptor) {
            lateinit var elementType: String
            lateinit var list: List<Any>
            loop@ while (true) {
                when (val i = decodeElementIndex(descriptor)) {
                    CompositeDecoder.DECODE_DONE -> break@loop
                    0 -> elementType = decodeStringElement(descriptor, i)
                    1 -> list = if (elementType != "emptyList") decodeSerializableElement(
                        descriptor,
                        i,
                        ListSerializer(AnySerializer(WaveBeansClassLoader.classForName(elementType) as KClass<Any>))
                    ) else emptyList()

                    else -> throw SerializationException("Unknown index $i")
                }
            }
            ListAsInputParams(list)
        }
    }

    override fun serialize(encoder: Encoder, value: ListAsInputParams) {
        encoder.encodeStructure(descriptor) {
            val firstEl = value.list.firstOrNull()?.javaClass?.kotlin?.jvmName
            encodeStringElement(descriptor, 0, firstEl ?: "emptyList")
            encodeSerializableElement(descriptor, 1, ListSerializer(AnySerializer()), value.list)
        }
    }
}
