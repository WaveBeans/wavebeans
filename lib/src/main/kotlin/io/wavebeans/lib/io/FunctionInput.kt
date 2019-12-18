package io.wavebeans.lib.io

import io.wavebeans.lib.BeanParams
import io.wavebeans.lib.BeanStream
import io.wavebeans.lib.SinglePartitionBean
import io.wavebeans.lib.SourceBean
import kotlinx.serialization.*
import kotlinx.serialization.internal.SerialClassDescImpl
import kotlin.reflect.jvm.jvmName

fun <T : Any> input(generator: (Long, Float) -> T?): BeanStream<T> = Input(InputParams(generator))

object InputParamsSerializer : KSerializer<InputParams<*>> {

    override val descriptor: SerialDescriptor = object : SerialClassDescImpl("InputParams") {
        init {
            addElement("generateFn")
        }
    }

    override fun deserialize(decoder: Decoder): InputParams<*> {
        val dec = decoder.beginStructure(descriptor)
        var funcClazzName: String? = null
        loop@ while (true) {
            when (val i = dec.decodeElementIndex(descriptor)) {
                CompositeDecoder.READ_DONE -> break@loop
                0 -> funcClazzName = dec.decodeStringElement(descriptor, i)
                else -> throw SerializationException("Unknown index $i")
            }
        }
        @Suppress("UNCHECKED_CAST") val funcByName = Class.forName(funcClazzName).newInstance() as (Long, Float) -> Any?
        return InputParams(funcByName)
    }

    override fun serialize(encoder: Encoder, obj: InputParams<*>) {
        val funcName = obj.generator::class.jvmName
        val structure = encoder.beginStructure(descriptor)
        structure.encodeStringElement(descriptor, 0, funcName)
        structure.endStructure(descriptor)
    }

}

@Serializable(with = InputParamsSerializer::class)
class InputParams<T : Any>(val generator: (Long, Float) -> T?) : BeanParams()

class Input<T : Any>(
        override val parameters: InputParams<T>
) : BeanStream<T>, SourceBean<T>, SinglePartitionBean {

    override fun asSequence(sampleRate: Float): Sequence<T> =
            (0..Long.MAX_VALUE).asSequence()
                    .map { parameters.generator(it, sampleRate) }
                    .takeWhile { it != null }
                    .map { it!! }

}