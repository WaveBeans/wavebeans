package io.wavebeans.execution.distributed

import io.wavebeans.execution.serializer.lambdaWrapper
import io.wavebeans.lib.stream.fft.FftSample
import io.wavebeans.lib.stream.window.Window
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
        element("zeroElFn", String.serializer().descriptor)
    }

    override fun deserialize(decoder: Decoder): Window<Any> {
        return decoder.decodeStructure(descriptor) {
            var size by notNull<Int>()
            var step by notNull<Int>()
            lateinit var elements: List<Any>
            lateinit var zeroEl: String
            @Suppress("UNCHECKED_CAST")
            loop@ while (true) {
                when (val i = decodeElementIndex(descriptor)) {
                    CompositeDecoder.DECODE_DONE -> break@loop
                    0 -> size = decodeIntElement(descriptor, i)
                    1 -> step = decodeIntElement(descriptor, i)
                    2 -> elements = decodeSerializableElement(descriptor, i, ListObjectSerializer)
                    3 -> zeroEl = decodeStringElement(descriptor, i)
                }
            }
            Window(size, step, elements, lambdaWrapper.deserialize(zeroEl))
        }
    }

    override fun serialize(encoder: Encoder, value: Window<Any>) {
        encoder.encodeStructure(descriptor) {
            encodeIntElement(descriptor, 0, value.size)
            encodeIntElement(descriptor, 1, value.step)
            encodeSerializableElement(descriptor, 2, ListObjectSerializer, value.elements)
            encodeStringElement(descriptor, 3, lambdaWrapper.serialize(value.zeroEl))
        }
    }
}