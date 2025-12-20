package io.wavebeans.execution.serializer

import io.wavebeans.lib.Fn
import io.wavebeans.lib.FnSerializer
import io.wavebeans.lib.className
import io.wavebeans.lib.io.CsvStreamOutputParams
import io.wavebeans.lib.wrap
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.*

/**
 * Serializer for [CsvStreamOutputParams].
 */
object CsvStreamOutputParamsSerializer : KSerializer<CsvStreamOutputParams<*, *>> {

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor(CsvStreamOutputParams::class.className()) {
            element("uri", String.serializer().descriptor)
            element("header", ListSerializer(String.serializer()).descriptor)
            element("encoding", String.serializer().descriptor)
            element("elementSerializer", FnSerializer.descriptor)
            element("suffix", FnSerializer.descriptor)
        }

    override fun deserialize(decoder: Decoder): CsvStreamOutputParams<*, *> {
        return decoder.decodeStructure(descriptor) {
            lateinit var uri: String
            lateinit var header: List<String>
            lateinit var elementSerializer: Fn<Triple<Long, Float, Any>, List<String>>
            lateinit var encoding: String
            lateinit var suffix: Fn<Any?, String>
            loop@ while (true) {
                when (val i = decodeElementIndex(descriptor)) {
                    0 -> uri = decodeStringElement(descriptor, i)
                    1 -> header = decodeSerializableElement(descriptor, i, ListSerializer(String.serializer()))
                    2 -> encoding = decodeStringElement(descriptor, i)
                    3 -> elementSerializer = decodeSerializableElement(descriptor, i, FnSerializer) as Fn<Triple<Long, Float, Any>, List<String>>
                    4 -> suffix = decodeSerializableElement(descriptor, i, FnSerializer) as Fn<Any?, String>
                    CompositeDecoder.DECODE_DONE -> break@loop
                    else -> throw SerializationException("Unknown index $i")
                }
            }
            CsvStreamOutputParams<Any, Any>(
                uri,
                header,
                { l, f, t -> elementSerializer.apply(Triple(l, f, t)) },
                encoding,
                { a -> suffix.apply(a) }
            )
        }
    }

    override fun serialize(encoder: Encoder, value: CsvStreamOutputParams<*, *>) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.uri)
            encodeSerializableElement(descriptor, 1, ListSerializer(String.serializer()), value.header)
            encodeStringElement(descriptor, 2, value.encoding)
            @Suppress("UNCHECKED_CAST")
            val elementSerializer = value.elementSerializer as (Long, Float, Any) -> List<String>
            encodeSerializableElement(
                descriptor,
                3,
                FnSerializer,
                wrap { t: Triple<Long, Float, Any> -> elementSerializer(t.first, t.second, t.third) } as Fn<Any?, Any?>
            )
            @Suppress("UNCHECKED_CAST")
            val suffix = value.suffix as (Any?) -> String
            encodeSerializableElement(
                descriptor,
                4,
                FnSerializer,
                wrap { a: Any? -> suffix(a) } as Fn<Any?, Any?>
            )
        }
    }

}
