package io.wavebeans.lib.io

import io.wavebeans.lib.BeanParams
import io.wavebeans.lib.SourceBean
import io.wavebeans.lib.WaveBeansClassLoader
import io.wavebeans.lib.stream.FiniteStream
import io.wavebeans.metrics.clazzTag
import io.wavebeans.metrics.samplesProcessedOnInputMetric
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.serializer
import java.util.concurrent.TimeUnit
import kotlin.reflect.jvm.jvmName

fun <T : Any> List<T>.input(): FiniteStream<T> {
    require(this.isNotEmpty()) { "Input list should not be empty" }
    return ListAsInput(ListAsInputParams(this))
}

// TODO serializable
class ListAsInputParams(
    val list: List<Any>
) : BeanParams {
    override fun toString(): String {
        return "ListAsInputParams(list=$list)"
    }
}

object ListAsInputParamsSerializer : KSerializer<ListAsInputParams> {

    private class PlainObjectSerializer(val type: String) : KSerializer<Any> {
        override val descriptor: SerialDescriptor
            get() = buildClassSerialDescriptor("Any") {}

        override fun deserialize(decoder: Decoder): Any {
            val s = serializer(WaveBeansClassLoader.classForName(type))
            return decoder.decodeSerializableValue(s)
        }

        override fun serialize(encoder: Encoder, value: Any) {
            val s = serializer(WaveBeansClassLoader.classForName(type))
            encoder.encodeSerializableValue(s, value)
        }
    }

    override val descriptor: SerialDescriptor
        get() = buildClassSerialDescriptor(ListAsInputParams::class.jvmName) {
            element("elementType", String.serializer().descriptor)
            element("elements", ListSerializer(PlainObjectSerializer("shouldn't matter")).descriptor)
        }

    override fun deserialize(decoder: Decoder): ListAsInputParams {
        return decoder.decodeStructure(descriptor) {
            lateinit var type: String
            lateinit var list: List<Any>
            @Suppress("UNCHECKED_CAST")
            loop@ while (true) {
                when (val i = decodeElementIndex(descriptor)) {
                    CompositeDecoder.DECODE_DONE -> break@loop
                    0 -> type = decodeStringElement(descriptor, i)
                    1 -> list = decodeSerializableElement(descriptor, i, ListSerializer(PlainObjectSerializer(type)))
                    else -> throw SerializationException("Unknown index $i")
                }
            }
            ListAsInputParams(list)
        }
    }

    override fun serialize(encoder: Encoder, value: ListAsInputParams) {
        encoder.encodeStructure(descriptor) {
            val elType = value.list.first()::class.jvmName
            encodeStringElement(descriptor, 0, elType)
            encodeSerializableElement(descriptor, 1, ListSerializer(PlainObjectSerializer(elType)), value.list)
        }
    }
}

class ListAsInput<T : Any>(
    override val parameters: ListAsInputParams
) : FiniteStream<T>, SourceBean<T> {

    private val samplesProcessed = samplesProcessedOnInputMetric.withTags(clazzTag to ListAsInput::class.jvmName)

    @Suppress("UNCHECKED_CAST")
    override fun asSequence(sampleRate: Float): Sequence<T> =
        parameters.list.asSequence().map { samplesProcessed.increment(); it as T }

    override fun length(timeUnit: TimeUnit): Long = 0

    override val desiredSampleRate: Float? = null

    override fun samplesCount(): Long = parameters.list.size.toLong()
}