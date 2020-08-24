package io.wavebeans.lib.io

import io.wavebeans.lib.BeanParams
import io.wavebeans.lib.BeanStream
import io.wavebeans.lib.SourceBean
import io.wavebeans.lib.WaveBeansClassLoader
import io.wavebeans.lib.stream.FiniteStream
import kotlinx.serialization.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import java.util.concurrent.TimeUnit
import kotlin.reflect.jvm.jvmName

fun <T : Any> List<T>.input(): FiniteStream<T> {
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
            get() = SerialDescriptor("Any") {}

        override fun deserialize(decoder: Decoder): Any {
            val s = serializerByTypeToken(WaveBeansClassLoader.classForName(type))
            return decoder.decode(s)
        }

        override fun serialize(encoder: Encoder, value: Any) {
            val s = serializerByTypeToken(WaveBeansClassLoader.classForName(type))
            encoder.encode(s, value)
        }
    }

    override val descriptor: SerialDescriptor
        get() = SerialDescriptor(ListAsInputParams::class.jvmName) {
            element("elementType", String.serializer().descriptor)
            element("elements", ListSerializer(PlainObjectSerializer("shouldn't matter")).descriptor)
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
                1 -> list = dec.decodeSerializableElement(descriptor, i, ListSerializer(PlainObjectSerializer(type!!)))
                else -> throw SerializationException("Unknown index $i")
            }
        }
        return ListAsInputParams(list!!)
    }

    override fun serialize(encoder: Encoder, value: ListAsInputParams) {
        val s = encoder.beginStructure(descriptor)
        val elType = value.list.first()::class.jvmName
        s.encodeStringElement(descriptor, 0, elType)
        s.encodeSerializableElement(descriptor, 1, ListSerializer(PlainObjectSerializer(elType)), value.list)
        s.endStructure(descriptor)
    }
}

class ListAsInput<T : Any>(
        override val parameters: ListAsInputParams
) : FiniteStream<T>, SourceBean<T> {

    @Suppress("UNCHECKED_CAST")
    override fun asSequence(sampleRate: Float): Sequence<T> = parameters.list.asSequence().map { it as T }

    override fun length(timeUnit: TimeUnit): Long = 0
}