package io.wavebeans.execution.serializer

import io.wavebeans.lib.*
import io.wavebeans.lib.io.WavFileOutputParams
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

/**
 * Serializer for [WavFileOutputParams]
 */
object WavFileOutputParamsSerializer : KSerializer<WavFileOutputParams<*>> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(WavFileOutputParams::class.className()) {
        element("uri", String.serializer().descriptor)
        element("bitDepth", Int.serializer().descriptor)
        element("numberOfChannels", Int.serializer().descriptor)
        element("suffix", FnSerializer.descriptor)
    }

    @Suppress("UNCHECKED_CAST")
    override fun deserialize(decoder: Decoder): WavFileOutputParams<*> {
        return decoder.decodeStructure(descriptor) {
            lateinit var uri: String
            var bitDepth: Int = 0
            var numberOfChannels: Int = 0
            lateinit var suffixFn: Fn<Any?, String>
            loop@ while (true) {
                when (val i = decodeElementIndex(descriptor)) {
                    CompositeDecoder.DECODE_DONE -> break@loop
                    0 -> uri = decodeStringElement(descriptor, i)
                    1 -> bitDepth = decodeIntElement(descriptor, i)
                    2 -> numberOfChannels = decodeIntElement(descriptor, i)
                    3 -> suffixFn = decodeSerializableElement(descriptor, i, FnSerializer) as Fn<Any?, String>
                    else -> throw SerializationException("Unknown index $i")
                }
            }
            WavFileOutputParams<Any>(
                uri,
                BitDepth.of(bitDepth),
                numberOfChannels,
                { a -> suffixFn.apply(a) }
            )
        }
    }

    override fun serialize(encoder: Encoder, value: WavFileOutputParams<*>) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.uri)
            encodeSerializableElement(descriptor, 1, Int.serializer(), value.bitDepth.bits)
            encodeSerializableElement(descriptor, 2, Int.serializer(), value.numberOfChannels)
            encodeSerializableElement(descriptor, 3, FnSerializer, wrap(value.suffix))
        }
    }
}
