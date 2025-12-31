package io.wavebeans.lib.io

import io.wavebeans.lib.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.*

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
    elementSerializer: ExecutionScope.(Long, Float, T) -> List<String>,
    encoding: String = "UTF-8",
    scope: ExecutionScope = EmptyScope,
): StreamOutput<T> {
    return CsvStreamOutput(
        this, CsvStreamOutputParams(
            uri = uri,
            header = header,
            elementSerializer = elementSerializer,
            encoding = encoding,
            scope = scope
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
    elementSerializer: ExecutionScope.(Long, Float, T) -> List<String>,
    suffix: ExecutionScope.(A?) -> String,
    encoding: String = "UTF-8",
    scope: ExecutionScope = EmptyScope,
): StreamOutput<Managed<OutputSignal, A, T>> {
    return CsvPartialStreamOutput(
        this,
        CsvStreamOutputParams(
            uri,
            header,
            elementSerializer,
            encoding,
            suffix,
            scope
        )
    )
}

/**
 * Parameters class for the [CsvStreamOutput] bean.
 */
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
    val elementSerializer: ExecutionScope.(Long, Float, T) -> List<String>,
    /**
     * Encoding to use to convert string to a byte array, by default `UTF-8`.
     */
    val encoding: String = "UTF-8",
    /**
     * The function that is based on argument of type [A] which is obtained from the moment the
     * [FlushOutputSignal] or [OpenGateOutputSignal] was generated. The suffix inserted after the name and
     * before the extension: `file:///home/user/my${suffix}.csv`
     */
    val suffix: ExecutionScope.(A?) -> String = { "" },
    val scope: ExecutionScope,
) : BeanParams

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
) : AbstractStreamOutput<T>(input), SinkBean<T>, SinglePartitionBean {

    override fun outputWriter(inputSequence: Sequence<T>, sampleRate: Float): Writer {
        var offset = 0L
        val writer = plainFileWriterDelegate<Unit>(parameters.uri)
        return object : AbstractWriter<T>(input, sampleRate, writer, CsvStreamOutput::class) {

            override fun header(): ByteArray? = csvHeader(parameters.header)

            override fun footer(): ByteArray? = null

            override fun serialize(element: T): ByteArray =
                serializeCsvElement(sampleRate, element, parameters.elementSerializer, parameters.scope) { offset++ }
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
) : AbstractStreamOutput<Managed<OutputSignal, A, T>>(input), SinkBean<Managed<OutputSignal, A, T>>,
    SinglePartitionBean {

    override fun outputWriter(inputSequence: Sequence<Managed<OutputSignal, A, T>>, sampleRate: Float): Writer {
        var offset = 0L
        val writer = suffixedFileWriterDelegate<A>(parameters.uri) { parameters.suffix.invoke(parameters.scope, it) }
        return object : AbstractPartialWriter<T, A>(input, sampleRate, writer, CsvStreamOutput::class) {

            override fun header(): ByteArray? = csvHeader(parameters.header)

            override fun footer(): ByteArray? = null

            override fun serialize(element: T): ByteArray =
                serializeCsvElement(sampleRate, element, parameters.elementSerializer, parameters.scope) { offset++ }

            override fun skip(element: T) {
                offset++
            }
        }
    }
}

private fun csvHeader(header: List<String>): ByteArray = (header.joinToString(",") + "\n").encodeToByteArray()

private fun <T : Any> serializeCsvElement(
    sampleRate: Float,
    element: T,
    elementSerializer: ExecutionScope.(Long, Float, T) -> List<String>,
    scope: ExecutionScope,
    getOffset: () -> Long,
): ByteArray {
    val seq = elementSerializer(scope, getOffset(), sampleRate, element)
    return (seq.joinToString(",") + "\n").encodeToByteArray()
}
