package io.wavebeans.execution.serializer

import io.wavebeans.lib.*
import io.wavebeans.lib.stream.window.WindowStreamParams
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
import kotlin.properties.Delegates.notNull

/**
 * Serializer for [WindowStreamParams].
 */
@Suppress("UNCHECKED_CAST")
object WindowStreamParamsSerializer : KSerializer<WindowStreamParams<*>> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(WindowStreamParams::class.className()) {
        element("scope", ExecutionScope.serializer().descriptor)
        element("windowSize", Int.serializer().descriptor)
        element("step", Int.serializer().descriptor)
        element("zeroElFn", String.serializer().descriptor)
    }

    override fun deserialize(decoder: Decoder): WindowStreamParams<*> {
        return decoder.decodeStructure(descriptor) {
            var scope: ExecutionScope = EmptyScope
            var windowSize by notNull<Int>()
            var step by notNull<Int>()
            lateinit var zeroElFn: String
            loop@ while (true) {
                when (val i = decodeElementIndex(descriptor)) {
                    CompositeDecoder.DECODE_DONE -> break@loop
                    0 -> scope = decodeSerializableElement(descriptor, i, ExecutionScope.serializer())
                    1 -> windowSize = decodeIntElement(descriptor, i)
                    2 -> step = decodeIntElement(descriptor, i)
                    3 -> zeroElFn = decodeStringElement(descriptor, i)
                    else -> throw SerializationException("Unknown index $i")
                }
            }
            WindowStreamParams(scope, windowSize, step, lambdaWrapper.deserialize2<Any, Any, Any>(zeroElFn))
        }
    }

    override fun serialize(encoder: Encoder, value: WindowStreamParams<*>) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, ExecutionScope.serializer(), value.scope)
            encodeIntElement(descriptor, 1, value.windowSize)
            encodeIntElement(descriptor, 2, value.step)
            encodeStringElement(descriptor, 3, lambdaWrapper.serialize(value.zeroElFn))
        }
    }
}
