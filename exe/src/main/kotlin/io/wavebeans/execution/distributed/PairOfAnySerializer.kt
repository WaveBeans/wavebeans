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
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
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
        return decoder.decodeStructure(descriptor) {
            lateinit var element1: Any
            lateinit var element1Class: KClass<*>
            lateinit var element2: Any
            lateinit var element2Class: KClass<*>
            loop@ while (true) {
                when (val i = decodeElementIndex(descriptor)) {
                    CompositeDecoder.DECODE_DONE -> break@loop
                    0 -> element1Class =
                        WaveBeansClassLoader.classForName(decodeStringElement(descriptor, i))

                    1 -> element1 = decodeSerializableElement(descriptor, i, AnySerializer(element1Class))
                    2 -> element2Class =
                        WaveBeansClassLoader.classForName(decodeStringElement(descriptor, i))

                    3 -> element2 = decodeSerializableElement(descriptor, i, AnySerializer(element2Class))
                    else -> throw SerializationException("Unknown index $i")
                }
            }
            Pair(element1, element2)
        }
    }

    override fun serialize(encoder: Encoder, value: Pair<Any, Any>) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, String.serializer(), value.first::class.jvmName)
            encodeSerializableElement(descriptor, 1, AnySerializer(), value.first)
            encodeSerializableElement(descriptor, 2, String.serializer(), value.second::class.jvmName)
            encodeSerializableElement(descriptor, 3, AnySerializer(), value.second)
        }
    }
}