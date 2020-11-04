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

/**
 * Streams the sample of any type into a CSV file by specified [uri]. The [header] is specified separately and added
 * as a first row. [elementSerializer] defines how you the rows are going to be stored.
 *
 * @param uri the URI the stream file to, i.e. `file:///home/user/output.csv`.
 * @param header the list of entries to put on the first row.
 * @param elementSerializer the function as instance of [Fn] of three arguments to convert it to a row (`List<String>`):
 *                          1. The `Long` specifies the offset of the row, always start at 0 and grows for any sample
 *                             being processed and passed through the output.
 *                          2. The `Float` specifies the sample rate the stream is being processed with.
 *                          3. The `T` keeps the sample to be converted to a row.
 * @param encoding encoding to use to convert string to a byte array, by default `UTF-8`.
 *
 * @param T the type of the sample in the stream, non-nullable.
 *
 * @return [StreamOutput] to run the further processing on.
 */
fun <T : Any> BeanStream<T>.toCsv(
        uri: String,
        header: List<String>,
        elementSerializer: Fn<Triple<Long, Float, T>, List<String>>,
        encoding: String = "UTF-8"
): StreamOutput<T> {
    return CsvStreamOutput(this, CsvStreamOutputParams(uri, header, elementSerializer, encoding))
}

/**
 * Streams the sample of any type into a CSV file by specified [uri]. The [header] is specified separately and added
 * as a first row. [elementSerializer] defines how you the rows are going to be stored.
 *
 * @param uri the URI the stream file to, i.e. `file:///home/user/output.csv`.
 * @param header the list of entries to put on the first row.
 * @param elementSerializer the function of three arguments to convert it to a row (`List<String>`):
 *                          1. The `Long` specifies the offset of the row, always start at 0 and grows for any sample
 *                             being processed and passed through the output.
 *                          2. The `Float` specifies the sample rate the stream is being processed with.
 *                          3. The `T` keeps the sample to be converted to a row.
 * @param encoding encoding to use to convert string to a byte array, by default `UTF-8`.
 *
 * @param T the type of the sample in the stream, non-nullable.
 *
 * @return [StreamOutput] to run the further processing on.
 */
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

/**
 * Streams the [Managed] sample of any type into a CSV file by specified [uri]. The [header] is specified separately and added
 * as a first row. [elementSerializer] defines how you the rows are going to be stored.
 *
 * The managing signal is of type [OutputSignal].
 *
 * @param uri the URI the stream file to, i.e. `file:///home/user/output.csv`.
 * @param header the list of entries to put on the first row.
 * @param elementSerializer the function as instance of [Fn] of three arguments to convert it to a row (`List<String>`):
 *                          1. The `Long` specifies the offset of the row, always start at 0 and grows for any sample
 *                             being processed and passed through the output.
 *                          2. The `Float` specifies the sample rate the stream is being processed with.
 *                          3. The `T` keeps the sample to be converted to a row.
 * @param suffix the function as instance of [Fn] that is based on argument of type [A] which is obtained from the moment the
 *               [FlushOutputSignal] or [OpenGateOutputSignal] was generated. The suffix inserted after the name and
 *               before the extension: `file:///home/user/my${suffix}.csv`
 * @param encoding encoding to use to convert string to a byte array, by default `UTF-8`.
 *
 * @param A      the type of the argument, use [Unit] if it's not applicable. Bear in mind that the [A] should be
 *               [Serializable] for some cases. Argument may be null if it wasn't specified, or on the very first run.
 * @param T      the type of the sample in the stream, non-nullable.
 *
 * @return [StreamOutput] to run the further processing on.
 */
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

/**
 * Streams the [Managed] sample of any type into a CSV file by specified [uri]. The [header] is specified separately and added
 * as a first row. [elementSerializer] defines how you the rows are going to be stored.
 *
 * The managing signal is of type [OutputSignal].
 *
 * @param uri the URI the stream file to, i.e. `file:///home/user/output.csv`.
 * @param header the list of entries to put on the first row.
 * @param elementSerializer the function of three arguments to convert it to a row (`List<String>`):
 *                          1. The `Long` specifies the offset of the row, always start at 0 and grows for any sample
 *                             being processed and passed through the output.
 *                          2. The `Float` specifies the sample rate the stream is being processed with.
 *                          3. The `T` keeps the sample to be converted to a row.
 * @param suffix the function that is based on argument of type [A] which is obtained from the moment the
 *               [FlushOutputSignal] or [OpenGateOutputSignal] was generated. The suffix inserted after the name and
 *               before the extension: `file:///home/user/my${suffix}.cwv`
 * @param encoding encoding to use to convert string to a byte array, by default `UTF-8`.
 *
 * @param A      the type of the argument, use [Unit] if it's not applicable. Bear in mind that the [A] should be
 *               [Serializable] for some cases. Argument may be null if it wasn't specified, or on the very first run.
 * @param T      the type of the sample in the stream, non-nullable.
 *
 * @return [StreamOutput] to run the further processing on.
 */
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

/**
 * Serializer for [CsvStreamOutputParams].
 */
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

/**
 * Parameters class for the [CsvStreamOutput] bean.
 */
@Serializable(with = CsvStreamOutputParamsSerializer::class)
data class CsvStreamOutputParams<A : Any, T : Any>(
        /**
         * The URI to stream to, i.e. `file:///home/user/my.csv`.
         */
        val uri: String,
        /**
         * The list of entries to put on the first row.
         */
        val header: List<String>,
        /**
         * The function of three arguments to convert it to a row (`List<String>`):
         *  1. The `Long` specifies the offset of the row, always start at 0 and grows for any sample
         *     being processed and passed through the output.
         *  2. The `Float` specifies the sample rate the stream is being processed with.
         *  3. The `T` keeps the sample to be converted to a row.
         */
        val elementSerializer: Fn<Triple<Long, Float, T>, List<String>>,
        /**
         * Encoding to use to convert string to a byte array, by default `UTF-8`.
         */
        val encoding: String = "UTF-8",
        /**
         * The function that is based on argument of type [A] which is obtained from the moment the
         * [FlushOutputSignal] or [OpenGateOutputSignal] was generated. The suffix inserted after the name and
         * before the extension: `file:///home/user/my${suffix}.csv`
         */
        val suffix: Fn<A?, String> = Fn.wrap { "" },
) : BeanParams()

/**
 * Streams the sample of any type into a CSV file.
 */
class CsvStreamOutput<T : Any>(
        /**
         * The stream to store into a csv-file.
         */
        override val input: BeanStream<T>,
        /**
         * Parameters to tune the stream output.
         */
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

/**
 * Streams the sample of any type into a CSV file. May flush the buffer with signals
 * [FlushOutputSignal], [OpenGateOutputSignal], [CloseGateOutputSignal].
 */
class CsvPartialStreamOutput<A : Any, T : Any>(
        /**
         * The stream of [Managed] samples to store into a csv-file.
         */
        override val input: BeanStream<Managed<OutputSignal, A, T>>,
        /**
         * Parameters to tune the stream output.
         */
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
