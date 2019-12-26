package io.wavebeans.lib.io

import io.wavebeans.lib.*
import kotlinx.serialization.*
import kotlinx.serialization.internal.ArrayListSerializer
import kotlinx.serialization.internal.SerialClassDescImpl
import kotlinx.serialization.internal.defaultSerializer
import java.net.URI
import java.nio.charset.Charset

fun <T : Any> BeanStream<T>.toCsv(
        uri: String,
        header: List<String>,
        elementSerializer: Fn<Triple<Long, Float, T>, List<String>>,
        encoding: String = "UTF-8"
): StreamOutput<T> {
    return CsvStreamOutput(this, CsvStreamOutputParams(uri, header, elementSerializer, encoding))
}

fun <T : Any> BeanStream<T>.toCsv(
        uri: String,
        header: List<String>,
        elementSerializer: (Triple<Long, Float, T>) -> List<String>,
        encoding: String = "UTF-8"
): StreamOutput<T> {
    return CsvStreamOutput(this, CsvStreamOutputParams(
            uri,
            header,
            Fn.wrap(elementSerializer),
            encoding)
    )
}

object CsvWindowStreamOutputParamsSerializer : KSerializer<CsvStreamOutputParams<*>> {

    override val descriptor: SerialDescriptor = object : SerialClassDescImpl("CsvWindowStreamOutputParams") {
        init {
            addElement("uri")
            addElement("header")
            addElement("elementSerializer")
            addElement("encoding")
        }
    }

    override fun deserialize(decoder: Decoder): CsvStreamOutputParams<*> {
        val dec = decoder.beginStructure(descriptor)
        var uri: String? = null
        var header: List<String>? = null
        var fn: Fn<*, *>? = null
        var encoding: String? = null
        loop@ while (true) {
            when (val i = dec.decodeElementIndex(descriptor)) {
                CompositeDecoder.READ_DONE -> break@loop
                0 -> uri = dec.decodeStringElement(descriptor, i)
                1 -> header = dec.decodeSerializableElement(descriptor, i, ArrayListSerializer(String::class.defaultSerializer()!!))
                2 -> fn = dec.decodeSerializableElement(descriptor, i, FnSerializer)
                3 -> encoding = dec.decodeStringElement(descriptor, i)
                else -> throw SerializationException("Unknown index $i")
            }
        }
        @Suppress("UNCHECKED_CAST")
        return CsvStreamOutputParams(uri!!, header!!, fn!! as Fn<Triple<Long, Float, Any>, List<String>>, encoding!!)
    }

    override fun serialize(encoder: Encoder, obj: CsvStreamOutputParams<*>) {
        val structure = encoder.beginStructure(descriptor)
        structure.encodeStringElement(descriptor, 0, obj.uri)
        structure.encodeSerializableElement(descriptor, 1, ArrayListSerializer(String::class.defaultSerializer()!!), obj.header)
        structure.encodeSerializableElement(descriptor, 2, FnSerializer, obj.elementSerializer)
        structure.encodeStringElement(descriptor, 3, obj.encoding)
        structure.endStructure(descriptor)
    }

}

@Serializable(with = CsvWindowStreamOutputParamsSerializer::class)
data class CsvStreamOutputParams<T : Any>(
        val uri: String,
        val header: List<String>,
        val elementSerializer: Fn<Triple<Long, Float, T>, List<String>>,
        val encoding: String = "UTF-8"
) : BeanParams()

class CsvStreamOutput<T : Any>(
        override val input: BeanStream<T>,
        override val parameters: CsvStreamOutputParams<T>
) : StreamOutput<T>, SinkBean<T>, SinglePartitionBean {

    override fun writer(sampleRate: Float): Writer {
        var offset = 0L
        val charset = Charset.forName(parameters.encoding)
        return object : FileWriter<T>(URI(parameters.uri), input, sampleRate) {

            override fun header(): ByteArray? = (parameters.header.joinToString(",") + "\n").toByteArray(charset)

            override fun footer(): ByteArray? = null

            override fun serialize(element: T): ByteArray {
                val seq = parameters.elementSerializer.apply(Triple(offset++, sampleRate, element))
                return (seq.joinToString(",") + "\n").toByteArray(charset)
            }

        }
    }
}