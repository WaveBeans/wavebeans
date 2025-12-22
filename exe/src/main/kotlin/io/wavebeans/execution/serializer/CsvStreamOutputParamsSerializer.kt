package io.wavebeans.execution.serializer

import io.wavebeans.lib.ExecutionScope
import io.wavebeans.lib.className
import io.wavebeans.lib.io.CsvStreamOutputParams
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
@Suppress("UNCHECKED_CAST")
object CsvStreamOutputParamsSerializer : KSerializer<CsvStreamOutputParams<*, *>> {

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor(CsvStreamOutputParams::class.className()) {
            element("uri", String.serializer().descriptor)
            element("header", ListSerializer(String.serializer()).descriptor)
            element("encoding", String.serializer().descriptor)
            element("elementSerializer", String.serializer().descriptor)
            element("suffix", String.serializer().descriptor)
            element("scope", ExecutionScope.serializer().descriptor)
        }

    override fun deserialize(decoder: Decoder): CsvStreamOutputParams<*, *> {
        return decoder.decodeStructure(descriptor) {
            lateinit var uri: String
            lateinit var header: List<String>
            lateinit var elementSerializer: String
            lateinit var encoding: String
            lateinit var suffix: String
            lateinit var scope: ExecutionScope
            loop@ while (true) {
                when (val i = decodeElementIndex(descriptor)) {
                    0 -> uri = decodeStringElement(descriptor, i)
                    1 -> header = decodeSerializableElement(descriptor, i, ListSerializer(String.serializer()))
                    2 -> encoding = decodeStringElement(descriptor, i)
                    3 -> elementSerializer = decodeStringElement(descriptor, i)
                    4 -> suffix = decodeStringElement(descriptor, i)
                    5 -> scope = decodeSerializableElement(descriptor, i, ExecutionScope.serializer())
                    CompositeDecoder.DECODE_DONE -> break@loop
                    else -> throw SerializationException("Unknown index $i")
                }
            }
            CsvStreamOutputParams<Any, Any>(
                uri,
                header,
                lambdaWrapper.deserialize4(elementSerializer),
                encoding,
                lambdaWrapper.deserialize2(suffix),
                scope,
            )
        }
    }

    override fun serialize(encoder: Encoder, value: CsvStreamOutputParams<*, *>) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.uri)
            encodeSerializableElement(descriptor, 1, ListSerializer(String.serializer()), value.header)
            encodeStringElement(descriptor, 2, value.encoding)
            encodeStringElement(
                descriptor,
                3,
                lambdaWrapper.serialize(value.elementSerializer)
            )
            encodeStringElement(
                descriptor,
                4,
                lambdaWrapper.serialize(value.suffix)
            )
            encodeSerializableElement(descriptor, 5, ExecutionScope.serializer(), value.scope)

        }
    }

}
