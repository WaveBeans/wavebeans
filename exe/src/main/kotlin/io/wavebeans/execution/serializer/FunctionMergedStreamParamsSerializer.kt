package io.wavebeans.execution.serializer

import io.wavebeans.lib.*
import io.wavebeans.lib.stream.FunctionMergedStreamParams
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
 * Serializer for [FunctionMergedStreamParams]
 */
object FunctionMergedStreamParamsSerializer : KSerializer<FunctionMergedStreamParams<*, *, *>> {

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor(FunctionMergedStreamParams::class.className()) {
            element("scope", ExecutionScope.serializer().descriptor)
            element("mergeFn", FnSerializer.descriptor)
        }

    override fun deserialize(decoder: Decoder): FunctionMergedStreamParams<*, *, *> {
        return decoder.decodeStructure(descriptor) {
            var scope: ExecutionScope = EmptyScope
            lateinit var func: Fn<Pair<Any?, Any?>, Any?>
            loop@ while (true) {
                when (val i = decodeElementIndex(descriptor)) {
                    CompositeDecoder.DECODE_DONE -> break@loop
                    0 -> scope = decodeSerializableElement(descriptor, i, ExecutionScope.serializer())
                    1 -> func =
                        decodeSerializableElement(descriptor, i, FnSerializer) as Fn<Pair<Any?, Any?>, Any?>

                    else -> throw SerializationException("Unknown index $i")
                }
            }
            FunctionMergedStreamParams(scope) { func.apply(it) }
        }
    }

    override fun serialize(encoder: Encoder, value: FunctionMergedStreamParams<*, *, *>) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, ExecutionScope.serializer(), value.scope)
            encodeSerializableElement(descriptor, 1, FnSerializer, wrap(value.merge))
        }
    }

}
