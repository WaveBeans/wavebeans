package io.wavebeans.lib.stream

import io.wavebeans.lib.AlterBean
import io.wavebeans.lib.BeanParams
import io.wavebeans.lib.BeanStream
import kotlinx.serialization.*
import kotlinx.serialization.internal.SerialClassDescImpl
import kotlin.reflect.jvm.jvmName

fun <T : Any, R : Any> BeanStream<T>.map(transform: (T) -> R): BeanStream<R> = MapStream(this, MapStreamParams(transform))

object MapStreamParamsSerializer : KSerializer<MapStreamParams<*, *>> {

    override val descriptor: SerialDescriptor = object : SerialClassDescImpl("MapStreamParams") {
        init {
            addElement("transformFn")
        }
    }

    override fun deserialize(decoder: Decoder): MapStreamParams<*, *> {
        val dec = decoder.beginStructure(descriptor)
        var funcClazzName: String? = null
        loop@ while (true) {
            when (val i = dec.decodeElementIndex(descriptor)) {
                CompositeDecoder.READ_DONE -> break@loop
                0 -> funcClazzName = dec.decodeStringElement(descriptor, i)
                else -> throw SerializationException("Unknown index $i")
            }
        }
        @Suppress("UNCHECKED_CAST") val funcByName = Class.forName(funcClazzName).newInstance() as (Any) -> Any
        return MapStreamParams(funcByName)
    }

    override fun serialize(encoder: Encoder, obj: MapStreamParams<*, *>) {
        val funcName = obj.transform::class.jvmName
        val structure = encoder.beginStructure(descriptor)
        structure.encodeStringElement(descriptor, 0, funcName)
        structure.endStructure(descriptor)
    }

}

@Serializable(with = MapStreamParamsSerializer::class)
class MapStreamParams<T : Any, R : Any>(val transform: (T) -> R) : BeanParams()

class MapStream<T : Any, R : Any>(
        override val input: BeanStream<T>,
        override val parameters: MapStreamParams<T, R>
) : BeanStream<R>, AlterBean<T, R> {

    override fun asSequence(sampleRate: Float): Sequence<R> =
            input.asSequence(sampleRate).map { parameters.transform(it) }

}