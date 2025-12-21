package io.wavebeans.execution.serializer

import io.wavebeans.execution.distributed.AnySerializer
import io.wavebeans.lib.className
import io.wavebeans.lib.io.ListAsInputParams
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

/**
 * Serializer for [ListAsInputParams].
 */
object ListAsInputParamsSerializer : KSerializer<ListAsInputParams> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(ListAsInputParams::class.className()) {
        element("list", ListSerializer(AnySerializer()).descriptor)
    }

    override fun deserialize(decoder: Decoder): ListAsInputParams {
        return decoder.decodeStructure(descriptor) {
            lateinit var list: List<Any>
            loop@ while (true) {
                when (val i = decodeElementIndex(descriptor)) {
                    CompositeDecoder.DECODE_DONE -> break@loop
                    0 -> list = decodeSerializableElement(descriptor, i, ListSerializer(AnySerializer()))
                    else -> throw SerializationException("Unknown index $i")
                }
            }
            ListAsInputParams(list)
        }
    }

    override fun serialize(encoder: Encoder, value: ListAsInputParams) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, ListSerializer(AnySerializer()), value.list)
        }
    }
}
