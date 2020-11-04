package io.wavebeans.lib.io

import io.wavebeans.lib.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.reflect.jvm.jvmName

/**
 * Streams the mono channel signal into the file with wav format, each sample is stored as unsigned 8 bit integer.
 * It is based on stream of [Sample] or [SampleArray].
 *
 * @param uri the URI to stream to, i.e. `file:///home/user/my.wav`
 * @param T   the type of the sample of input and hence output streams.
 *
 * @return [StreamOutput] to run the processing on.
 */
inline fun <reified T : Any> BeanStream<T>.toMono8bitWav(uri: String): StreamOutput<T> =
        this.toWav<T, Unit, T>(uri, BitDepth.BIT_8, 1)

/**
 * Streams the mono channel signal into the file with wav format, each sample is stored as signed 16 bit
 * integer with LE byte order. It is based on stream of [Sample] or [SampleArray].
 *
 * @param uri the URI to stream to, i.e. `file:///home/user/my.wav`
 * @param T   the type of the sample of input and hence output streams. Either [Sample] or [SampleArray].
 *
 * @return [StreamOutput] to run the processing on.
 */
inline fun <reified T : Any> BeanStream<T>.toMono16bitWav(uri: String): StreamOutput<T> =
        this.toWav<T, Unit, T>(uri, BitDepth.BIT_16, 1)

/**
 * Streams the mono channel signal into the file with wav format, each sample is stored as signed 24 bit
 * integer with LE byte order. It is based on stream of [Sample] or [SampleArray].
 *
 * @param uri the URI to stream to, i.e. `file:///home/user/my.wav`
 * @param T   the type of the sample of input and hence output streams.
 *
 * @return [StreamOutput] to run the processing on.
 */
inline fun <reified T : Any> BeanStream<T>.toMono24bitWav(uri: String): StreamOutput<T> =
        this.toWav<T, Unit, T>(uri, BitDepth.BIT_24, 1)

/**
 * Streams the mono channel signal into the file with wav format, each sample is stored as signed 32 bit
 * integer with LE byte order. It is based on stream of [Sample] or [SampleArray].
 *
 * @param uri the URI to stream to, i.e. `file:///home/user/my.wav`
 * @param T   the type of the sample of input and hence output streams.
 *
 * @return [StreamOutput] to run the processing on.
 */
inline fun <reified T : Any> BeanStream<T>.toMono32bitWav(uri: String): StreamOutput<T> =
        this.toWav<T, Unit, T>(uri, BitDepth.BIT_32, 1)

/**
 * Streams the mono channel signal into the file with wav format, each sample is stored as unsigned 8 bit
 * integer. It is based on stream of [Managed] <[Sample]> or <[SampleArray]>. The signal [FlushOutputSignal] is
 * used to dump the current "buffer" and create the new one, the argument [A] may be passed at this moment. The [suffix]
 * function is used to generate the next suffix of the output.
 *
 * The managing signal is of type [OutputSignal].
 *
 * @param uri    the URI to stream to, i.e. `file:///home/user/my.wav`
 * @param suffix the function that based on argument of type [A] which is obtained from the moment the
 *               [FlushOutputSignal] or [OpenGateOutputSignal] was emitted. The suffix inserted after the name
 *               and before the extension: `file:///home/user/my${suffix}.wav`
 * @param A      the type of the argument, use [Unit] if it's not applicable. Bear in mind that the [A] should be
 *               [Serializable] for some cases. Argument may be null if it wasn't specified, or on the very first run.
 * @param T      the type of the sample of input and output streams. Either [Sample] or [SampleArray].
 *
 * @return [StreamOutput] to run the processing on.
 */
inline fun <A : Any, reified T : Any> BeanStream<Managed<OutputSignal, A, T>>.toMono8bitWav(
        uri: String,
        noinline suffix: (A?) -> String
): StreamOutput<Managed<OutputSignal, A, T>> = toWav<Managed<OutputSignal, A, T>, A, T>(uri, BitDepth.BIT_8, 1, suffix)

/**
 * Streams the mono channel signal into the file with wav format, each sample is stored as signed 16 bit integer
 * with LE byte order. It is based on stream of [Managed] <[Sample]> or <[SampleArray]>. The signal [FlushOutputSignal] is
 * used to dump the current "buffer" and create the new one, the argument [A] may be passed at this moment. The [suffix]
 * function is used to generate the next suffix of the output.
 *
 * The managing signal is of type [OutputSignal].
 *
 * @param uri    the URI to stream to, i.e. `file:///home/user/my.wav`
 * @param suffix the function that based on argument of type [A] which is obtained from the moment the
 *               [FlushOutputSignal] was generated. The suffix inserted after the name and before the extension:
 *               `file:///home/user/my${suffix}.wav`
 * @param A      the type of the argument, use [Unit] if it's not applicable. Bear in mind that the [A] should be
 *               [Serializable] for some cases. Argument may be null if it wasn't specified, or on the very first run.
 * @param T      the type of the sample of input and output streams. Either [Sample] or [SampleArray].
 *
 * @return [StreamOutput] to run the processing on.
 */
inline fun <A : Any, reified T : Any> BeanStream<Managed<OutputSignal, A, T>>.toMono16bitWav(
        uri: String,
        noinline suffix: (A?) -> String
): StreamOutput<Managed<OutputSignal, A, T>> = toWav<Managed<OutputSignal, A, T>, A, T>(uri, BitDepth.BIT_16, 1, suffix)

/**
 * Streams the mono channel signal into the file with wav format, each sample is stored as signed 24 bit integer
 * with LE byte order. It is based on stream of [Managed] <[Sample]> or <[SampleArray]>. The signal [FlushOutputSignal] is
 * used to dump the current "buffer" and create the new one, the argument [A] may be passed at this moment. The [suffix]
 * function is used to generate the next suffix of the output.
 *
 * The managing signal is of type [OutputSignal].
 *
 * @param uri    the URI to stream to, i.e. `file:///home/user/my.wav`
 * @param suffix the function that based on argument of type [A] which is obtained from the moment the
 *               [FlushOutputSignal] was generated. The suffix inserted after the name and before the extension:
 *               `file:///home/user/my${suffix}.wav`
 * @param A      the type of the argument, use [Unit] if it's not applicable. Bear in mind that the [A] should be
 *               [Serializable] for some cases. Argument may be null if it wasn't specified, or on the very first run.
 * @param T      the type of the sample of input and output streams. Either [Sample] or [SampleArray].
 *
 * @return [StreamOutput] to run the processing on.
 */
inline fun <A : Any, reified T : Any> BeanStream<Managed<OutputSignal, A, T>>.toMono24bitWav(
        uri: String,
        noinline suffix: (A?) -> String
): StreamOutput<Managed<OutputSignal, A, T>> = toWav<Managed<OutputSignal, A, T>, A, T>(uri, BitDepth.BIT_24, 1, suffix)

/**
 * Streams the mono channel signal into the file with wav format, each sample is stored as signed 32 bit integer
 * with LE byte order. It is based on stream of [Managed] <[Sample]> or <[SampleArray]>. The signal [FlushOutputSignal] is
 * used to dump the current "buffer" and create the new one, the argument [A] may be passed at this moment. The [suffix]
 * function is used to generate the next suffix of the output.
 *
 * The managing signal is of type [OutputSignal].
 *
 * @param uri    the URI to stream to, i.e. `file:///home/user/my.wav`
 * @param suffix the function that based on argument of type [A] which is obtained from the moment the
 *               [FlushOutputSignal] was generated. The suffix inserted after the name and before the extension:
 *               `file:///home/user/my${suffix}.wav`
 * @param A      the type of the argument, use [Unit] if it's not applicable. Bear in mind that the [A] should be
 *               [Serializable] for some cases. Argument may be null if it wasn't specified, or on the very first run.
 * @param T      the type of the sample of input and output streams. Either [Sample] or [SampleArray].
 *
 * @return [StreamOutput] to run the processing on.
 */
inline fun <A : Any, reified T : Any> BeanStream<Managed<OutputSignal, A, T>>.toMono32bitWav(
        uri: String,
        noinline suffix: (A?) -> String
): StreamOutput<Managed<OutputSignal, A, T>> = toWav<Managed<OutputSignal, A, T>, A, T>(uri, BitDepth.BIT_32, 1, suffix)

/**
 * Streams signal into the file with wav format. It is based on **Simple** or [Managed] stream of <[Sample]>
 * or <[SampleArray]>. In case of [Managed] stream the signal [FlushOutputSignal] is used to dump the current "buffer"
 * and create the new one, the argument [A] may be passed at this moment. The [suffix] function is used to generate the
 * next suffix of the output. If the [suffix] is specified the stream is enforced to be [Managed], otherwise **Simple**.
 *
 * The managing signal is of type [OutputSignal].
 *
 * **For internal use only.**
 *
 * @param uri              the URI to stream to, i.e. `file:///home/user/my.wav`.
 * @param bitDepth         the numeric type to use to store the sample of [Sample] or [SampleArray].
 * @param numberOfChannels number of cahnnels to use, only mono (1) is supported at the moment.
 * @param suffix           the function that based on argument of type [A] which is obtained from the moment the
 *                         [FlushOutputSignal] was generated. The suffix inserted after the name and before the extension:
 *                         `file:///home/user/my${suffix}.wav`.
 *
 * @param A      the type of the argument, use [Unit] if it's not applicable. Bear in mind that the [A] should be
 *               [Serializable] for some cases. Argument may be null if it wasn't specified, or on the very first run.
 * @param T      the type of the sample of input and output streams. Either [Sample] or [SampleArray].
 * @param R      the type of the stream to return.
 *
 * @return [StreamOutput] of type [R] to run the further processing on.
 */
@Suppress("UNCHECKED_CAST")
inline fun <R : Any, A : Any, reified T : Any> BeanStream<R>.toWav(
        uri: String,
        bitDepth: BitDepth,
        numberOfChannels: Int,
        noinline suffix: ((A?) -> String)? = null
): StreamOutput<R> {
    when {
        (T::class == Sample::class || T::class == SampleArray::class) && suffix != null -> {
            return WavPartialFileOutput(
                    this as BeanStream<Managed<OutputSignal, A, Any>>,
                    WavFileOutputParams(uri, bitDepth, numberOfChannels, Fn.wrap(suffix))
            ) as StreamOutput<R>
        }
        (T::class == Sample::class || T::class == SampleArray::class) && suffix == null -> {
            return WavFileOutput(
                    this as BeanStream<Any>,
                    WavFileOutputParams(uri, bitDepth, numberOfChannels)
            ) as StreamOutput<R>
        }
        else -> throw UnsupportedOperationException("Sample class ${T::class} and " +
                "suffix=$suffix is not supported for streaming Wav output")
    }
}

/**
 * [BeanParams] to create the [WavFileOutput] or [WavPartialFileOutput] with.
 *
 * @param [A] if the [suffix] function is used, then the type of its argument, otherwise you mau use [Unit].
 */
@Serializable(with = WavFileOutputParamsSerializer::class)
data class WavFileOutputParams<A : Any>(
        /**
         * The URI to stream to, i.e. `file:///home/user/my.wav`.
         */
        val uri: String,
        /**
         * The numeric type to use to store the samples. The byte order is LE.
         */
        val bitDepth: BitDepth,
        /**
         * Number of channels to store in wav-file.
         */
        val numberOfChannels: Int,
        /**
         * [Fn] function to generate suffix is applicable for the stream.
         */
        val suffix: Fn<A?, String> = Fn.wrap { "" },
) : BeanParams()

object WavFileOutputParamsSerializer: KSerializer<WavFileOutputParams<*>> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(WavFileOutputParams::class.jvmName) {
        element("uri", String.serializer().descriptor)
        element("bitDepth", Int.serializer().descriptor)
        element("numberOfChannels", Int.serializer().descriptor)
        element("suffix", FnSerializer.descriptor)
    }

    override fun deserialize(decoder: Decoder): WavFileOutputParams<*> {
        val dec = decoder.beginStructure(descriptor)
        var uri: String? = null
        var bitDepth: Int? = null
        var numberOfChannels: Int? = null
        var suffix: Fn<*, *>? = null
        loop@ while (true) {
            when (val i = dec.decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break@loop
                0 -> uri = dec.decodeStringElement(descriptor, i)
                1 -> bitDepth = dec.decodeIntElement(descriptor, i)
                2 -> numberOfChannels = dec.decodeIntElement(descriptor, i)
                3 -> suffix = dec.decodeSerializableElement(descriptor, i, FnSerializer)
                else -> throw SerializationException("Unknown index $i")
            }
        }
        @Suppress("UNCHECKED_CAST")
        return WavFileOutputParams(
                uri!!,
                BitDepth.of(bitDepth!!),
                numberOfChannels!!,
                suffix!! as Fn<Any?, String>
        )
    }

    override fun serialize(encoder: Encoder, value: WavFileOutputParams<*>) {
        val structure = encoder.beginStructure(descriptor)
        structure.encodeStringElement(descriptor, 0, value.uri)
        structure.encodeSerializableElement(descriptor, 1, Int.serializer(), value.bitDepth.bits)
        structure.encodeSerializableElement(descriptor, 2, Int.serializer(), value.numberOfChannels)
        structure.encodeSerializableElement(descriptor, 3, FnSerializer, value.suffix)
        structure.endStructure(descriptor)
    }

}

/**
 * Performs the output of the [stream] to a single wav-file. Uses [WavWriter] ot perform the actual writing.
 * The [params] of type [WavFileOutputParams] are used to tune the output, except the [WavFileOutputParams.suffix] is
 * never called.
 */
class WavFileOutput(
        /**
         * The stream to store into a wav-file. Can be of type [Sample] or [SampleArray].
         */
        val stream: BeanStream<Any>,
        /**
         * Parameters to tune the stream output.
         */
        val params: WavFileOutputParams<Unit>
) : StreamOutput<Any>, SinglePartitionBean {

    override val input: Bean<Any> = stream

    override val parameters: BeanParams = params

    override fun writer(sampleRate: Float): Writer =
            WavWriter(
                    stream,
                    params.bitDepth,
                    sampleRate,
                    params.numberOfChannels,
                    plainFileWriterDelegate<Unit>(params.uri),
                    WavFileOutput::class
            )

}

/**
 * Performs the output of the [stream] to multiple wav-files. Uses [WavPartialWriter] ot perform the actual writing.
 * The [params] of type [WavFileOutputParams] are used to tune the output, the [WavFileOutputParams.suffix] is called
 * whenever the [FlushOutputSignal] was called in order to generate new URI.
 */
class WavPartialFileOutput<A : Any>(
        /**
         * The [Managed] stream to store into a wav-file. Sample type can be one of [Sample] or [SampleArray].
         */
        val stream: BeanStream<Managed<OutputSignal, A, Any>>,
        /**
         * Parameters to tune the stream output.
         */
        val params: WavFileOutputParams<A>
) : StreamOutput<Managed<OutputSignal, A, Any>>, SinglePartitionBean {

    override val input: Bean<Managed<OutputSignal, A, Any>> = stream

    override val parameters: BeanParams = params

    override fun writer(sampleRate: Float): Writer =
            WavPartialWriter(
                    stream,
                    params.bitDepth,
                    sampleRate,
                    params.numberOfChannels,
                    suffixedFileWriterDelegate(params.uri) { params.suffix.apply(it) },
                    WavPartialFileOutput::class
            )

}

