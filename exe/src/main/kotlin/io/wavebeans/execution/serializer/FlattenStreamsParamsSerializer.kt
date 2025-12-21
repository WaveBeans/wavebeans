package io.wavebeans.execution.serializer

import io.wavebeans.lib.*
import io.wavebeans.lib.stream.FlattenStreamsParams
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
 * Serializer for [FlattenStreamsParams].
 */
@Suppress("UNCHECKED_CAST")
object FlattenStreamsParamsSerializer : KSerializer<FlattenStreamsParams<*, *>> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor(FlattenStreamsParams::class.className()) {
            element("scope", ExecutionScope.serializer().descriptor)
            element("map", FnSerializer.descriptor)
        }

    override fun deserialize(decoder: Decoder): FlattenStreamsParams<*, *> {
        return decoder.decodeStructure(descriptor) {
            var scope: ExecutionScope = EmptyScope
            lateinit var map: Fn<Any, Iterable<Any>>
            loop@ while (true) {
                when (val i = decodeElementIndex(descriptor)) {
                    CompositeDecoder.DECODE_DONE -> break@loop
                    0 -> scope = decodeSerializableElement(descriptor, i, ExecutionScope.serializer())
                    1 -> map = decodeSerializableElement(descriptor, i, FnSerializer) as Fn<Any, Iterable<Any>>
                    else -> throw SerializationException("Unknown index $i")
                }
            }
            FlattenStreamsParams<Any, Any>(scope) { map.apply(it) }
        }
    }

    override fun serialize(encoder: Encoder, value: FlattenStreamsParams<*, *>) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, ExecutionScope.serializer(), value.scope)
            encodeSerializableElement(descriptor, 1, FnSerializer, wrap(value.map))
        }
    }
}
