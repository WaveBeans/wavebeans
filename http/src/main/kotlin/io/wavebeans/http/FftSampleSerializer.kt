package io.wavebeans.http

import io.wavebeans.lib.stream.fft.FftSample
import kotlinx.serialization.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlin.reflect.jvm.jvmName

object FftSampleSerializer : KSerializer<FftSample> {

    private val magnitudeSerializer = ListSerializer(Double.serializer())
    private val phaseSerializer = ListSerializer(Double.serializer())
    private val frequencyDescriptor = ListSerializer(Double.serializer()).descriptor

    override val descriptor: SerialDescriptor = SerialDescriptor(FftSample::class.jvmName) {
        element("index", Long.serializer().descriptor)
        element("binCount", Int.serializer().descriptor)
        element("samplesCount", Int.serializer().descriptor)
        element("sampleRate", Float.serializer().descriptor)
        element("magnitude", magnitudeSerializer.descriptor)
        element("phase", phaseSerializer.descriptor)
        element("frequency", frequencyDescriptor)
        element("time", Long.serializer().descriptor)
    }

    override fun deserialize(decoder: Decoder): FftSample =
            throw UnsupportedOperationException("This serializer can only be used for serialization!")

    override fun serialize(encoder: Encoder, value: FftSample) {
        val s = encoder.beginStructure(descriptor)
        s.encodeLongElement(descriptor, 0, value.index)
        s.encodeIntElement(descriptor, 1, value.binCount)
        s.encodeIntElement(descriptor, 2, value.samplesCount)
        s.encodeFloatElement(descriptor, 3, value.sampleRate)
        s.encodeSerializableElement(descriptor, 4, magnitudeSerializer, value.magnitude().toList())
        s.encodeSerializableElement(descriptor, 5, phaseSerializer, value.phase().toList())
        s.encodeSerializableElement(descriptor, 6, ListSerializer(Double.serializer()), value.frequency().toList())
        s.encodeLongElement(descriptor, 7, value.time())
        s.endStructure(descriptor)
    }
}

