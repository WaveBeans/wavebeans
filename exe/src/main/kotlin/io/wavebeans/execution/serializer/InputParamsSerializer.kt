package io.wavebeans.execution.serializer

import io.wavebeans.lib.ExecutionScope
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
@Suppress("UNCHECKED_CAST")
object InputParamsSerializer : KSerializer<InputParams<*>> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(InputParams::class.className()) {
        element("generateFn", String.serializer().descriptor)
        element("sampleRate", Float.serializer().nullable.descriptor)
        element("scope", ExecutionScope.serializer().descriptor)
    }

    override fun deserialize(decoder: Decoder): InputParams<*> {
        return decoder.decodeStructure(descriptor) {
            var sampleRate: Float? = null
            lateinit var func: String
            lateinit var scope: ExecutionScope
            @Suppress("UNCHECKED_CAST")
            loop@ while (true) {
                when (val i = decodeElementIndex(descriptor)) {
                    CompositeDecoder.DECODE_DONE -> break@loop
                    0 -> func = decodeStringElement(descriptor, i)
                    1 -> sampleRate = decodeNullableSerializableElement(descriptor, i, Float.serializer().nullable)
                    2 -> scope = decodeSerializableElement(descriptor, i, ExecutionScope.serializer())
                    else -> throw SerializationException("Unknown index $i")
                }
            }
            InputParams(
                lambdaWrapper.deserialize3<ExecutionScope, Long, Float, Any>(func),
                scope,
                sampleRate
            )
        }
    }

    override fun serialize(encoder: Encoder, value: InputParams<*>) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, lambdaWrapper.serialize(value.generator))
            encodeNullableSerializableElement(descriptor, 1, Float.serializer().nullable, value.sampleRate)
            encodeSerializableElement(descriptor, 2, ExecutionScope.serializer(), value.scope)
        }
    }

}

