package io.wavebeans.lib.io

import io.wavebeans.lib.*
import kotlinx.serialization.*
import kotlinx.serialization.internal.SerialClassDescImpl

fun <T : Any> input(generator: (Pair<Long, Float>) -> T?): BeanStream<T> = Input(InputParams(Fn.wrap(generator)))
fun <T : Any> input(generator: Fn<Pair<Long, Float>, T?>): BeanStream<T> = Input(InputParams(generator))

object InputParamsSerializer : KSerializer<InputParams<*>> {

    override val descriptor: SerialDescriptor = object : SerialClassDescImpl("InputParams") {
        init {
            addElement("generateFn")
        }
    }

    override fun deserialize(decoder: Decoder): InputParams<*> {
        val dec = decoder.beginStructure(descriptor)
        var func: Fn<Pair<Long, Float>, Any?>? = null
        @Suppress("UNCHECKED_CAST")
        loop@ while (true) {
            when (val i = dec.decodeElementIndex(descriptor)) {
                CompositeDecoder.READ_DONE -> break@loop
                0 -> func = dec.decodeSerializableElement(descriptor, i, FnSerializer) as Fn<Pair<Long, Float>, Any?>
                else -> throw SerializationException("Unknown index $i")
            }
        }
        return InputParams(func!!)
    }

    override fun serialize(encoder: Encoder, obj: InputParams<*>) {
        val structure = encoder.beginStructure(descriptor)
        structure.encodeSerializableElement(descriptor, 0, FnSerializer, obj.generator)
        structure.endStructure(descriptor)
    }

}

@Serializable(with = InputParamsSerializer::class)
class InputParams<T : Any>(val generator: Fn<Pair<Long, Float>, T?>) : BeanParams()

class Input<T : Any>(
        override val parameters: InputParams<T>
) : BeanStream<T>, SourceBean<T>, SinglePartitionBean {

    override fun asSequence(sampleRate: Float): Sequence<T> =
            (0..Long.MAX_VALUE).asSequence()
                    .map { parameters.generator.apply(Pair(it, sampleRate)) }
                    .takeWhile { it != null }
                    .map { it!! }

}