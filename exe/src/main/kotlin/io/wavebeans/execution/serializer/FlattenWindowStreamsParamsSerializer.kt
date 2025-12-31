package io.wavebeans.execution.serializer

import io.wavebeans.lib.*
import io.wavebeans.lib.stream.FlattenWindowStreamsParams
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
 * Serializer for [FlattenWindowStreamsParams].
 */
@Suppress("UNCHECKED_CAST")
object FlattenWindowStreamsParamsSerializer : KSerializer<FlattenWindowStreamsParams<*>> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor(FlattenWindowStreamsParams::class.className()) {
            element("scope", ExecutionScope.serializer().descriptor)
            element("overlapResolve", String.serializer().descriptor)
        }

    override fun deserialize(decoder: Decoder): FlattenWindowStreamsParams<*> {
        return decoder.decodeStructure(descriptor) {
            var scope: ExecutionScope = EmptyScope
            lateinit var overlapResolve: String
            loop@ while (true) {
                when (val i = decodeElementIndex(descriptor)) {
                    CompositeDecoder.DECODE_DONE -> break@loop
                    0 -> scope = decodeSerializableElement(descriptor, i, ExecutionScope.serializer())
                    1 -> overlapResolve = decodeStringElement(descriptor, i)
                    else -> throw SerializationException("Unknown index $i")
                }
            }
            FlattenWindowStreamsParams<Any>(scope, lambdaWrapper.deserialize2<ExecutionScope, Pair<Any, Any>, Any>(overlapResolve))
        }
    }

    override fun serialize(encoder: Encoder, value: FlattenWindowStreamsParams<*>) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, ExecutionScope.serializer(), value.scope)
            encodeStringElement(descriptor, 1, lambdaWrapper.serialize(value.overlapResolve))
        }
    }
}
