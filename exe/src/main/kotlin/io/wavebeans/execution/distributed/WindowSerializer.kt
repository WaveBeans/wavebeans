package io.wavebeans.execution.distributed

import io.wavebeans.lib.WaveBeansClassLoader
import io.wavebeans.lib.stream.fft.FftSample
import io.wavebeans.lib.stream.window.Window
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlin.properties.Delegates
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
            lateinit var zeroEl: (() -> Any)
            @Suppress("UNCHECKED_CAST")
            loop@ while (true) {
                when (val i = decodeElementIndex(descriptor)) {
                    CompositeDecoder.DECODE_DONE -> break@loop
                    0 -> size = decodeIntElement(descriptor, i)
                    1 -> step = decodeIntElement(descriptor, i)
                    2 -> elements = decodeSerializableElement(descriptor, i, ListObjectSerializer)
                    3 -> {
                        val clazz = WaveBeansClassLoader.classForName(decodeStringElement(descriptor, i))
                        val constructor = clazz.declaredConstructors.first { it.parameterCount == 0 }
                        constructor.isAccessible = true
                        zeroEl = constructor.newInstance() as () -> Any
                    }
                }
            }
            Window(size, step, elements, zeroEl)
        }
    }

    override fun serialize(encoder: Encoder, value: Window<Any>) {
        encoder.encodeStructure(descriptor) {
            encodeIntElement(descriptor, 0, value.size)
            encodeIntElement(descriptor, 1, value.step)
            encodeSerializableElement(descriptor, 2, ListObjectSerializer, value.elements)
            encodeStringElement(descriptor, 3, value.zeroEl::class.jvmName)
        }
    }
}