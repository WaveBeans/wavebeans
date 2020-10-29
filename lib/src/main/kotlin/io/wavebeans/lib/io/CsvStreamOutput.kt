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

fun <A : Any, T : Any> BeanStream<Managed<OutputSignal, A, T>>.toCsv(
        uri: String,
        header: List<String>,
        elementSerializer: Fn<Triple<Long, Float, T>, List<String>>,
        suffix: Fn<A?, String>,
        encoding: String = "UTF-8",
): StreamOutput<Managed<OutputSignal, A, T>> {
    return CsvPartialStreamOutput(
            this,
            CsvStreamOutputParams(
                    uri,
                    header,
                    elementSerializer,
                    encoding,
                    suffix
            )
    )
}

fun <A : Any, T : Any> BeanStream<Managed<OutputSignal, A, T>>.toCsv(
        uri: String,
        header: List<String>,
        elementSerializer: (Triple<Long, Float, T>) -> List<String>,
        suffix: (A?) -> String,
        encoding: String = "UTF-8",
): StreamOutput<Managed<OutputSignal, A, T>> {
    return this.toCsv(
            uri,
            header,
            Fn.wrap(elementSerializer),
            Fn.wrap(suffix),
            encoding
    )
}


object CsvStreamOutputParamsSerializer : KSerializer<CsvStreamOutputParams<*, *>> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(CsvStreamOutputParams::class.jvmName) {
        element("uri", String.serializer().descriptor)
        element("header", String.serializer().descriptor)
        element("elementSerializer", FnSerializer.descriptor)
        element("encoding", String.serializer().descriptor)
        element("suffix", FnSerializer.descriptor)
    }

    override fun deserialize(decoder: Decoder): CsvStreamOutputParams<*, *> {
        val dec = decoder.beginStructure(descriptor)
        var uri: String? = null
        var header: List<String>? = null
        var fn: Fn<*, *>? = null
        var encoding: String? = null
        var suffix: Fn<*, *>? = null
        loop@ while (true) {
            when (val i = dec.decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break@loop
                0 -> uri = dec.decodeStringElement(descriptor, i)
                1 -> header = dec.decodeSerializableElement(descriptor, i, ListSerializer(String.serializer()))
                2 -> fn = dec.decodeSerializableElement(descriptor, i, FnSerializer)
                3 -> encoding = dec.decodeStringElement(descriptor, i)
                4 -> suffix = dec.decodeSerializableElement(descriptor, i, FnSerializer)
                else -> throw SerializationException("Unknown index $i")
            }
        }
        @Suppress("UNCHECKED_CAST")
        return CsvStreamOutputParams(
                uri!!,
                header!!,
                fn!! as Fn<Triple<Long, Float, Any>, List<String>>,
                encoding!!,
                suffix!! as Fn<Any?, String>
        )
    }

    override fun serialize(encoder: Encoder, value: CsvStreamOutputParams<*, *>) {
        val structure = encoder.beginStructure(descriptor)
        structure.encodeStringElement(descriptor, 0, value.uri)
        structure.encodeSerializableElement(descriptor, 1, ListSerializer(String.serializer()), value.header)
        structure.encodeSerializableElement(descriptor, 2, FnSerializer, value.elementSerializer)
        structure.encodeStringElement(descriptor, 3, value.encoding)
        structure.encodeSerializableElement(descriptor, 4, FnSerializer, value.suffix)
        structure.endStructure(descriptor)
    }

}

@Serializable(with = CsvStreamOutputParamsSerializer::class)
data class CsvStreamOutputParams<A : Any, T : Any>(
        val uri: String,
        val header: List<String>,
        val elementSerializer: Fn<Triple<Long, Float, T>, List<String>>,
        val encoding: String = "UTF-8",
        val suffix: Fn<A?, String> = Fn.wrap { "" },
) : BeanParams()

class CsvStreamOutput<T : Any>(
        override val input: BeanStream<T>,
        override val parameters: CsvStreamOutputParams<Unit, T>
) : StreamOutput<T>, SinkBean<T>, SinglePartitionBean {

    override fun writer(sampleRate: Float): Writer {
        var offset = 0L
        val charset = Charset.forName(parameters.encoding)
        val writer = plainFileWriterDelegate<Unit>(parameters.uri)
        return object : AbstractWriter<T>(input, sampleRate, writer, CsvStreamOutput::class) {

            override fun header(): ByteArray? = csvHeader(parameters.header, charset)

            override fun footer(): ByteArray? = null

            override fun serialize(element: T): ByteArray =
                    serializeCsvElement(sampleRate, element, charset, parameters.elementSerializer) { offset++ }
        }
    }
}

class CsvPartialStreamOutput<A : Any, T : Any>(
        override val input: BeanStream<Managed<OutputSignal, A, T>>,
        override val parameters: CsvStreamOutputParams<A, T>
) : StreamOutput<Managed<OutputSignal, A, T>>, SinkBean<Managed<OutputSignal, A, T>>, SinglePartitionBean {

    override fun writer(sampleRate: Float): Writer {
        var offset = 0L
        val charset = Charset.forName(parameters.encoding)
        val writer = suffixedFileWriterDelegate<A>(parameters.uri) { parameters.suffix.apply(it) }
        return object : AbstractPartialWriter<T, A>(input, sampleRate, writer, CsvStreamOutput::class) {

            override fun header(): ByteArray? = csvHeader(parameters.header, charset)

            override fun footer(): ByteArray? = null

            override fun serialize(element: T): ByteArray =
                    serializeCsvElement(sampleRate, element, charset, parameters.elementSerializer) { offset++ }

            override fun skip(element: T) {
                offset++
            }
        }
    }
}

private fun csvHeader(header: List<String>, charset: Charset): ByteArray = (header.joinToString(",") + "\n").toByteArray(charset)

private fun <T : Any> serializeCsvElement(
        sampleRate: Float,
        element: T,
        charset: Charset,
        elementSerializer: Fn<Triple<Long, Float, T>, List<String>>,
        getOffset: () -> Long
): ByteArray {
    val seq = elementSerializer.apply(Triple(getOffset(), sampleRate, element))
    return (seq.joinToString(",") + "\n").toByteArray(charset)
}
