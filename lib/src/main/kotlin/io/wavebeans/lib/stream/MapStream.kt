package io.wavebeans.lib.stream

import io.wavebeans.lib.*
import kotlinx.serialization.*
import kotlinx.serialization.internal.SerialClassDescImpl

fun <T : Any, R : Any> BeanStream<T>.map(transform: (T) -> R): BeanStream<R> = this.map(Fn.wrap(transform))
fun <T : Any, R : Any> BeanStream<T>.map(transform: Fn<T, R>): BeanStream<R> = MapStream(this, MapStreamParams(transform))

object MapStreamParamsSerializer : KSerializer<MapStreamParams<*, *>> {

    override val descriptor: SerialDescriptor = object : SerialClassDescImpl("MapStreamParams") {
        init {
            addElement("transformFn")
        }
    }

    override fun deserialize(decoder: Decoder): MapStreamParams<*, *> {
        val dec = decoder.beginStructure(descriptor)
        var fn: Fn<Any, Any>? = null
        @Suppress("UNCHECKED_CAST")
        loop@ while (true) {
            when (val i = dec.decodeElementIndex(descriptor)) {
                CompositeDecoder.READ_DONE -> break@loop
                0 -> fn = dec.decodeSerializableElement(descriptor, i, FnSerializer) as Fn<Any, Any>
                else -> throw SerializationException("Unknown index $i")
            }
        }
        return MapStreamParams(fn!!)
    }

    override fun serialize(encoder: Encoder, obj: MapStreamParams<*, *>) {
        val structure = encoder.beginStructure(descriptor)
        structure.encodeSerializableElement(descriptor, 0, FnSerializer, obj.transform)
        structure.endStructure(descriptor)
    }

}

@Serializable(with = MapStreamParamsSerializer::class)
class MapStreamParams<T : Any, R : Any>(val transform: Fn<T, R>) : BeanParams()

class MapStream<T : Any, R : Any>(
        override val input: BeanStream<T>,
        override val parameters: MapStreamParams<T, R>
) : BeanStream<R>, AlterBean<T, R> {

    override fun asSequence(sampleRate: Float): Sequence<R> =
            input.asSequence(sampleRate).map { parameters.transform.apply(it) }

}