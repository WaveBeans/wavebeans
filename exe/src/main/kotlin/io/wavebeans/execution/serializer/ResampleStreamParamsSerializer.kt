package io.wavebeans.execution.serializer

import io.wavebeans.lib.className
import io.wavebeans.lib.stream.ResampleStreamParams
import io.wavebeans.lib.stream.ResamplingArgument
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
 * Serializer for [ResampleStreamParams].
 */
@Suppress("UNCHECKED_CAST")
object ResampleStreamParamsSerializer : KSerializer<ResampleStreamParams<*>> {

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor(ResampleStreamParamsSerializer::class.className()) {
            element("to", Float.serializer().nullable.descriptor)
            element("resampleFn", String.serializer().descriptor)
        }

    override fun deserialize(decoder: Decoder): ResampleStreamParams<*> {
        return decoder.decodeStructure(descriptor) {
            var to: Float? = null
            lateinit var resampleFn: String
            @Suppress("UNCHECKED_CAST")
            loop@ while (true) {
                when (val i = decodeElementIndex(descriptor)) {
                    CompositeDecoder.DECODE_DONE -> break@loop
                    0 -> to = decodeNullableSerializableElement(descriptor, i, Float.serializer().nullable)
                    1 -> resampleFn = decodeStringElement(descriptor, i)

                    else -> throw SerializationException("Unknown index $i")
                }
            }

            ResampleStreamParams(to, lambdaWrapper.deserialize<ResamplingArgument<Any>, Sequence<Any>>(resampleFn))
        }
    }

    override fun serialize(encoder: Encoder, value: ResampleStreamParams<*>) {
        encoder.encodeStructure(descriptor) {
            encodeNullableSerializableElement(descriptor, 0, Float.serializer().nullable, value.to)
            encodeStringElement(descriptor, 1, lambdaWrapper.serialize(value.resampleFn))
        }
    }
}

