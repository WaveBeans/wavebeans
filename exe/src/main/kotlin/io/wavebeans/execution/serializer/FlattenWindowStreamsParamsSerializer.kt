package io.wavebeans.execution.serializer

import io.wavebeans.lib.*
import io.wavebeans.lib.stream.FlattenWindowStreamsParams
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
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
            element("overlapResolve", FnSerializer.descriptor)
        }

    override fun deserialize(decoder: Decoder): FlattenWindowStreamsParams<*> {
        return decoder.decodeStructure(descriptor) {
            var scope: ExecutionScope = EmptyScope
            lateinit var overlapResolve: Fn<Pair<Any, Any>, Any>
            loop@ while (true) {
                when (val i = decodeElementIndex(descriptor)) {
                    CompositeDecoder.DECODE_DONE -> break@loop
                    0 -> scope = decodeSerializableElement(descriptor, i, ExecutionScope.serializer())
                    1 -> overlapResolve = decodeSerializableElement(descriptor, i, FnSerializer) as Fn<Pair<Any, Any>, Any>
                    else -> throw SerializationException("Unknown index $i")
                }
            }
            FlattenWindowStreamsParams<Any>(scope) { overlapResolve.apply(it) }
        }
    }

    override fun serialize(encoder: Encoder, value: FlattenWindowStreamsParams<*>) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, ExecutionScope.serializer(), value.scope)
            encodeSerializableElement(descriptor, 1, FnSerializer, wrap(value.overlapResolve))
        }
    }
}
