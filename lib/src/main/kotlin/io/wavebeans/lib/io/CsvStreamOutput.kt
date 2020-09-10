package io.wavebeans.lib.io

import io.wavebeans.lib.*
import kotlinx.serialization.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.net.URI
import java.nio.charset.Charset
import kotlin.reflect.jvm.jvmName

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
    return this.toCsv(
            uri,
            header,
            Fn.wrap(elementSerializer),
            encoding
    )
}

object CsvWindowStreamOutputParamsSerializer : KSerializer<CsvStreamOutputParams<*>> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(CsvStreamOutputParams::class.jvmName) {
        element("uri", String.serializer().descriptor)
        element("header", String.serializer().descriptor)
        element("elementSerializer", String.serializer().descriptor)
        element("encoding", String.serializer().descriptor)
    }

    override fun deserialize(decoder: Decoder): CsvStreamOutputParams<*> {
        val dec = decoder.beginStructure(descriptor)
        var uri: String? = null
        var header: List<String>? = null
        var fn: Fn<*, *>? = null
        var encoding: String? = null
        loop@ while (true) {
            when (val i = dec.decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break@loop
                0 -> uri = dec.decodeStringElement(descriptor, i)
                1 -> header = dec.decodeSerializableElement(descriptor, i, ListSerializer(String.serializer()))
                2 -> fn = dec.decodeSerializableElement(descriptor, i, FnSerializer)
                3 -> encoding = dec.decodeStringElement(descriptor, i)
                else -> throw SerializationException("Unknown index $i")
            }
        }
        @Suppress("UNCHECKED_CAST")
        return CsvStreamOutputParams(uri!!, header!!, fn!! as Fn<Triple<Long, Float, Any>, List<String>>, encoding!!)
    }

    override fun serialize(encoder: Encoder, value: CsvStreamOutputParams<*>) {
        val structure = encoder.beginStructure(descriptor)
        structure.encodeStringElement(descriptor, 0, value.uri)
        structure.encodeSerializableElement(descriptor, 1, ListSerializer(String.serializer()), value.header)
        structure.encodeSerializableElement(descriptor, 2, FnSerializer, value.elementSerializer)
        structure.encodeStringElement(descriptor, 3, value.encoding)
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
        val writer = FileWriterDelegate(URI(parameters.uri))
        return object : AbstractWriter<T>(input, sampleRate, writer, this::class) {

            override fun header(): ByteArray? = (parameters.header.joinToString(",") + "\n").toByteArray(charset)

            override fun footer(): ByteArray? = null

            override fun serialize(element: T): ByteArray {
                val seq = parameters.elementSerializer.apply(Triple(offset++, sampleRate, element))
                return (seq.joinToString(",") + "\n").toByteArray(charset)
            }

        }
    }
}