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
object WindowStreamParamsSerializer : KSerializer<WindowStreamParams<*>> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(WindowStreamParams::class.className()) {
        element("scope", ExecutionScope.serializer().descriptor)
        element("windowSize", Int.serializer().descriptor)
        element("step", Int.serializer().descriptor)
        element("zeroElFn", FnSerializer.descriptor)
    }

    override fun deserialize(decoder: Decoder): WindowStreamParams<*> {
        return decoder.decodeStructure(descriptor) {
            var scope: ExecutionScope = EmptyScope
            var windowSize by notNull<Int>()
            var step by notNull<Int>()
            lateinit var zeroElFn: Fn<Unit, Any>
            loop@ while (true) {
                when (val i = decodeElementIndex(descriptor)) {
                    CompositeDecoder.DECODE_DONE -> break@loop
                    0 -> scope = decodeSerializableElement(descriptor, i, ExecutionScope.serializer())
                    1 -> windowSize = decodeIntElement(descriptor, i)
                    2 -> step = decodeIntElement(descriptor, i)
                    3 -> zeroElFn = decodeSerializableElement(descriptor, i, FnSerializer) as Fn<Unit, Any>
                    else -> throw SerializationException("Unknown index $i")
                }
            }
            WindowStreamParams<Any>(scope, windowSize, step) { zeroElFn.apply(it) }
        }
    }

    override fun serialize(encoder: Encoder, value: WindowStreamParams<*>) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, ExecutionScope.serializer(), value.scope)
            encodeIntElement(descriptor, 1, value.windowSize)
            encodeIntElement(descriptor, 2, value.step)
            encodeSerializableElement(descriptor, 3, FnSerializer, wrap(value.zeroElFn))
        }
    }
}
