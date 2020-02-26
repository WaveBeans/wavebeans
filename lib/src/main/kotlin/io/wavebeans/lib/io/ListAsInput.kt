package io.wavebeans.lib.io

import io.wavebeans.lib.BeanParams
import io.wavebeans.lib.BeanStream
import io.wavebeans.lib.SourceBean
import kotlinx.serialization.*
import kotlinx.serialization.internal.ArrayListSerializer
import kotlinx.serialization.internal.SerialClassDescImpl
import kotlin.reflect.jvm.jvmName

fun <T : Any> List<T>.input(): BeanStream<T> {
    require(this.isNotEmpty()) { "Input list should not be empty" }
    return ListAsInput(ListAsInputParams(this))
}

class ListAsInputParams(
        val list: List<Any>
) : BeanParams() {
    override fun toString(): String {
        return "ListAsInputParams(list=$list)"
    }
}

object ListAsInputParamsSerializer : KSerializer<ListAsInputParams> {

    private class PlainObjectSerializer(val type: String) : KSerializer<Any> {
        override val descriptor: SerialDescriptor
            get() = object : SerialClassDescImpl("any") {}

        override fun deserialize(decoder: Decoder): Any {
            val s = serializerByTypeToken(Class.forName(type))
            return decoder.decode(s)
        }

        override fun serialize(encoder: Encoder, obj: Any) {
            val s = serializerByTypeToken(Class.forName(type))
            encoder.encode(s, obj)
        }
    }

    override val descriptor: SerialDescriptor
        get() = object : SerialClassDescImpl("ListAsInputParams") {
            init {
                addElement("elementType")
                addElement("elements")
            }
        }

    override fun deserialize(decoder: Decoder): ListAsInputParams {
        val dec = decoder.beginStructure(descriptor)
        var type: String? = null
        var list: List<Any>? = null
        @Suppress("UNCHECKED_CAST")
        loop@ while (true) {
            when (val i = dec.decodeElementIndex(descriptor)) {
                CompositeDecoder.READ_DONE -> break@loop
                0 -> type = dec.decodeStringElement(descriptor, i)
                1 -> list = dec.decodeSerializableElement(descriptor, i, ArrayListSerializer(PlainObjectSerializer(type!!)))
                else -> throw SerializationException("Unknown index $i")
            }
        }
        return ListAsInputParams(list!!)
    }

    override fun serialize(encoder: Encoder, obj: ListAsInputParams) {
        val s = encoder.beginStructure(descriptor)
        val elType = obj.list.first()::class.jvmName
        s.encodeStringElement(descriptor, 0, elType)
        s.encodeSerializableElement(descriptor, 1, ArrayListSerializer(PlainObjectSerializer(elType)), obj.list)
        s.endStructure(descriptor)
    }
}

class ListAsInput<T : Any>(
        override val parameters: ListAsInputParams
) : BeanStream<T>, SourceBean<T> {

    @Suppress("UNCHECKED_CAST")
    override fun asSequence(sampleRate: Float): Sequence<T> = parameters.list.asSequence().map { it as T }
}