package io.wavebeans.execution.distributed

import io.wavebeans.execution.serializer.Fn
import io.wavebeans.execution.serializer.FnSerializer
import io.wavebeans.lib.stream.fft.FftSample
import io.wavebeans.lib.stream.window.Window
import io.wavebeans.execution.serializer.wrap
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.*
import kotlin.properties.Delegates.notNull
import kotlin.reflect.jvm.jvmName

object WindowOfAnySerializer : KSerializer<Window<Any>> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(FftSample::class.jvmName) {
        element("size", Int.serializer().descriptor)
        element("step", Int.serializer().descriptor)
        element("elements", ListObjectSerializer.descriptor)
        element("zeroElFn", FnSerializer.descriptor)
    }

    override fun deserialize(decoder: Decoder): Window<Any> {
        return decoder.decodeStructure(descriptor) {
            var size by notNull<Int>()
            var step by notNull<Int>()
            lateinit var elements: List<Any>
            lateinit var zeroEl: Fn<Unit, Any>
            @Suppress("UNCHECKED_CAST")
            loop@ while (true) {
                when (val i = decodeElementIndex(descriptor)) {
                    CompositeDecoder.DECODE_DONE -> break@loop
                    0 -> size = decodeIntElement(descriptor, i)
                    1 -> step = decodeIntElement(descriptor, i)
                    2 -> elements = decodeSerializableElement(descriptor, i, ListObjectSerializer)
                    3 -> zeroEl = decodeSerializableElement(descriptor, i, FnSerializer) as Fn<Unit, Any>
                }
            }
            Window(size, step, elements) { zeroEl.apply(it) }
        }
    }

    override fun serialize(encoder: Encoder, value: Window<Any>) {
        encoder.encodeStructure(descriptor) {
            encodeIntElement(descriptor, 0, value.size)
            encodeIntElement(descriptor, 1, value.step)
            encodeSerializableElement(descriptor, 2, ListObjectSerializer, value.elements)
            encodeSerializableElement(descriptor, 3, FnSerializer, wrap(value.zeroEl))
        }
    }
}