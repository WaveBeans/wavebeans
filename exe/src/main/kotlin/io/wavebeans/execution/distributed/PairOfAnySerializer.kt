package io.wavebeans.execution.distributed

import io.wavebeans.lib.WaveBeansClassLoader
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName

object PairOfAnySerializer : KSerializer<Pair<Any, Any>> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(PairOfAnySerializer::class.jvmName) {
        element("element1Class", String.serializer().descriptor)
        element("element1", AnySerializer().descriptor)
        element("element2Class", String.serializer().descriptor)
        element("element2", AnySerializer().descriptor)
    }

    override fun deserialize(decoder: Decoder): Pair<Any, Any> {
        val dec = decoder.beginStructure(descriptor)
        var element1: Any? = null
        var element1Class: KClass<*>? = null
        var element2: Any? = null
        var element2Class: KClass<*>? = null
        loop@ while (true) {
            when (val i = dec.decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break@loop
                0 -> element1Class = WaveBeansClassLoader.classForName(dec.decodeStringElement(descriptor, i)).kotlin
                1 -> element1 = dec.decodeSerializableElement(descriptor, i, AnySerializer(element1Class!!))
                2 -> element2Class = WaveBeansClassLoader.classForName(dec.decodeStringElement(descriptor, i)).kotlin
                3 -> element2 = dec.decodeSerializableElement(descriptor, i, AnySerializer(element2Class!!))
                else -> throw SerializationException("Unknown index $i")
            }
        }
        dec.endStructure(descriptor)
        return Pair(element1!!, element2!!)
    }

    override fun serialize(encoder: Encoder, value: Pair<Any, Any>) {
        val s = encoder.beginStructure(descriptor)
        s.encodeSerializableElement(descriptor, 0, String.serializer(), value.first::class.jvmName)
        s.encodeSerializableElement(descriptor, 1, AnySerializer(), value.first)
        s.encodeSerializableElement(descriptor, 2, String.serializer(), value.second::class.jvmName)
        s.encodeSerializableElement(descriptor, 3, AnySerializer(), value.second)
        s.endStructure(descriptor)
    }
}