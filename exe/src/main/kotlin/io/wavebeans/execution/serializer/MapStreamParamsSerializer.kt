package io.wavebeans.execution.serializer

import io.wavebeans.lib.*
import io.wavebeans.lib.stream.MapStreamParams
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.*

@Suppress("UNCHECKED_CAST")
object MapStreamParamsSerializer : KSerializer<MapStreamParams<*, *>> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(MapStreamParams::class.className()) {
        element("scope", ExecutionScope.serializer().descriptor)
        element("transformFn", String.serializer().descriptor)
    }

    override fun deserialize(decoder: Decoder): MapStreamParams<*, *> {
        return decoder.decodeStructure(descriptor) {
            lateinit var fn: String
            lateinit var scope: ExecutionScope
            @Suppress("UNCHECKED_CAST")
            loop@ while (true) {
                when (val i = decodeElementIndex(descriptor)) {
                    CompositeDecoder.DECODE_DONE -> break@loop
                    0 -> scope = decodeSerializableElement(descriptor, i, ExecutionScope.serializer())
                    1 -> fn = decodeStringElement(descriptor, i)
                    else -> throw SerializationException("Unknown index $i")
                }
            }
            MapStreamParams(scope, lambdaWrapper.deserialize2<Any, Any, Any>(fn))
        }
    }

    override fun serialize(encoder: Encoder, value: MapStreamParams<*, *>) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, ExecutionScope.serializer(), value.scope)
            encodeStringElement(descriptor, 1, lambdaWrapper.serialize(value.transform))
        }
    }
}
