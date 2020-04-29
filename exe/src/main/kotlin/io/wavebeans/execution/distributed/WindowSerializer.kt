package io.wavebeans.execution.distributed

import io.wavebeans.lib.WaveBeansClassLoader
import io.wavebeans.lib.stream.fft.FftSample
import io.wavebeans.lib.stream.window.Window
import kotlinx.serialization.*
import kotlinx.serialization.builtins.serializer
import kotlin.reflect.jvm.jvmName

object WindowOfAnySerializer : KSerializer<Window<Any>> {

    override val descriptor: SerialDescriptor = SerialDescriptor(FftSample::class.jvmName) {
        element("size", Int.serializer().descriptor)
        element("step", Int.serializer().descriptor)
        element("elements", ListObjectSerializer.descriptor)
        element("zeroElFn", String.serializer().descriptor)
    }

    override fun deserialize(decoder: Decoder): Window<Any> {
        val dec = decoder.beginStructure(descriptor)
        var size: Int? = null
        var step: Int? = null
        var elements: List<Any>? = null
        var zeroEl: (() -> Any)? = null
        @Suppress("UNCHECKED_CAST")
        loop@ while (true) {
            when (val i = dec.decodeElementIndex(descriptor)) {
                CompositeDecoder.READ_DONE -> break@loop
                0 -> size = dec.decodeIntElement(descriptor, i)
                1 -> step = dec.decodeIntElement(descriptor, i)
                2 -> elements = dec.decodeSerializableElement(descriptor, i, ListObjectSerializer)
                3 -> {
                    val clazz = WaveBeansClassLoader.classForName(dec.decodeStringElement(descriptor, i))
                    val constructor = clazz.declaredConstructors.first { it.parameterCount == 0 }
                    constructor.isAccessible = true
                    zeroEl = constructor.newInstance() as () -> Any
                }
            }
        }
        dec.endStructure(descriptor)
        return Window(size!!, step!!, elements!!, zeroEl!!)
    }

    override fun serialize(encoder: Encoder, value: Window<Any>) {
        val s = encoder.beginStructure(descriptor)
        s.encodeIntElement(descriptor, 0, value.size)
        s.encodeIntElement(descriptor, 1, value.step)
        s.encodeSerializableElement(descriptor, 2, ListObjectSerializer, value.elements)
        s.encodeStringElement(descriptor, 3, value.zeroEl::class.jvmName)
        s.endStructure(descriptor)
    }
}