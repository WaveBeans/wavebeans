package io.wavebeans.execution.serializer

import io.wavebeans.execution.distributed.AnySerializer
import io.wavebeans.lib.*
import io.wavebeans.lib.stream.MapStreamParams
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.*

object MapStreamParamsSerializer : KSerializer<MapStreamParams<*, *>> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(MapStreamParams::class.className()) {
        element("scope", AnySerializer().descriptor)
        element("transformFn", FnSerializer.descriptor)
    }

    override fun deserialize(decoder: Decoder): MapStreamParams<*, *> {
        return decoder.decodeStructure(descriptor) {
            lateinit var fn: Fn<Any, Any>
            lateinit var scope: ExecutionScope
            @Suppress("UNCHECKED_CAST")
            loop@ while (true) {
                when (val i = decodeElementIndex(descriptor)) {
                    CompositeDecoder.DECODE_DONE -> break@loop
                    0 -> scope = decodeSerializableElement(descriptor, i, AnySerializer()) as ExecutionScope
                    1 -> fn = decodeSerializableElement(descriptor, i, FnSerializer) as Fn<Any, Any>
                    else -> throw SerializationException("Unknown index $i")
                }
            }
            MapStreamParams<Any, Any>(scope) { fn.apply(it) }
        }
    }

    override fun serialize(encoder: Encoder, value: MapStreamParams<*, *>) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, AnySerializer(), value.scope)
            encodeSerializableElement(descriptor, 1, FnSerializer, wrap(value.transform))
        }
    }
}
