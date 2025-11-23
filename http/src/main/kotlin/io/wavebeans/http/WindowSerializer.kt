package io.wavebeans.http

import io.wavebeans.lib.stream.fft.FftSample
import io.wavebeans.lib.stream.window.Window
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.encodeStructure
import kotlin.reflect.jvm.jvmName

object WindowSerializer : KSerializer<Window<Any>> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(FftSample::class.jvmName) {
        element("size", Int.serializer().descriptor)
        element("step", Int.serializer().descriptor)
        element("elements", PlainObjectSerializer.descriptor)
        element("sampleType", String.serializer().descriptor)
    }

    override fun deserialize(decoder: Decoder): Window<Any> =
            throw UnsupportedOperationException("This serializer can only be used for serialization!")

    override fun serialize(encoder: Encoder, value: Window<Any>) {
        encoder.encodeStructure(descriptor) {
            encodeIntElement(descriptor, 0, value.size)
            encodeIntElement(descriptor, 1, value.step)
            encodeSerializableElement(descriptor, 2, ListSerializer(PlainObjectSerializer), value.elements)
            encodeStringElement(descriptor, 3, value.elements.first()::class.jvmName)
        }
    }
}