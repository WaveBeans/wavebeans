package io.wavebeans.execution.serializer

import io.wavebeans.lib.className
import io.wavebeans.lib.io.InputParams
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

/**
 * Serializer for [InputParams]
 */
object InputParamsSerializer : KSerializer<InputParams<*>> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(InputParams::class.className()) {
        element("generateFn", FnSerializer.descriptor)
        element("sampleRate", Float.serializer().nullable.descriptor)
    }

    override fun deserialize(decoder: Decoder): InputParams<*> {
        return decoder.decodeStructure(descriptor) {
            var sampleRate: Float? = null
            lateinit var func: Fn<Pair<Long, Float>, Any?>
            @Suppress("UNCHECKED_CAST")
            loop@ while (true) {
                when (val i = decodeElementIndex(descriptor)) {
                    CompositeDecoder.DECODE_DONE -> break@loop
                    0 -> func =
                        decodeSerializableElement(descriptor, i, FnSerializer) as Fn<Pair<Long, Float>, Any?>

                    1 -> sampleRate = decodeNullableSerializableElement(descriptor, i, Float.serializer().nullable)
                    else -> throw SerializationException("Unknown index $i")
                }
            }
            InputParams({ a, b -> func.apply(a to b) }, sampleRate)
        }
    }

    override fun serialize(encoder: Encoder, value: InputParams<*>) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, FnSerializer, wrap(value.generator))
            encodeNullableSerializableElement(descriptor, 1, Float.serializer().nullable, value.sampleRate)
        }
    }

}

