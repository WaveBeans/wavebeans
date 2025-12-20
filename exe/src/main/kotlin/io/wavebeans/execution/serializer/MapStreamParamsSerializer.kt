package io.wavebeans.execution.serializer

import io.wavebeans.lib.Fn
import io.wavebeans.lib.FnSerializer
import io.wavebeans.lib.className
import io.wavebeans.lib.stream.MapStreamParams
import io.wavebeans.lib.wrap
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

object MapStreamParamsSerializer : KSerializer<MapStreamParams<*, *>> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(MapStreamParams::class.className()) {
        element("transformFn", FnSerializer.descriptor)
    }

    override fun deserialize(decoder: Decoder): MapStreamParams<*, *> {
        return decoder.decodeStructure(descriptor) {
            lateinit var fn: Fn<Any, Any>
            @Suppress("UNCHECKED_CAST")
            loop@ while (true) {
                when (val i = decodeElementIndex(descriptor)) {
                    CompositeDecoder.DECODE_DONE -> break@loop
                    0 -> fn = decodeSerializableElement(descriptor, i, FnSerializer) as Fn<Any, Any>
                    else -> throw SerializationException("Unknown index $i")
                }
            }
            MapStreamParams<Any, Any> { fn.apply(it) }
        }
    }

    override fun serialize(encoder: Encoder, value: MapStreamParams<*, *>) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, FnSerializer, wrap(value.transform))
        }
    }
}
