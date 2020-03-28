package io.wavebeans.http

import io.wavebeans.lib.stream.fft.FftSample
import io.wavebeans.lib.stream.window.Window
import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlin.reflect.jvm.jvmName

object WindowSerializer : KSerializer<Window<Any>> {

    override val descriptor: SerialDescriptor = SerialDescriptor(FftSample::class.jvmName) {
        element("size", Int.serializer().descriptor)
        element("step", Int.serializer().descriptor)
        element("elements", PlainObjectSerializer.descriptor)
        element("sampleType", String.serializer().descriptor)
    }

    override fun deserialize(decoder: Decoder): Window<Any> =
            throw UnsupportedOperationException("This serializer can only be used for serialization!")

    override fun serialize(encoder: Encoder, value: Window<Any>) {
        val s = encoder.beginStructure(descriptor)
        s.encodeIntElement(descriptor, 0, value.size)
        s.encodeIntElement(descriptor, 1, value.step)
        s.encodeSerializableElement(descriptor, 2, ListSerializer(PlainObjectSerializer), value.elements)
        s.encodeStringElement(descriptor, 3, value.elements.first()::class.jvmName)
        s.endStructure(descriptor)
    }
}