package io.wavebeans.lib.io

import io.wavebeans.lib.*
import kotlinx.serialization.Serializable


inline fun <reified T : Any> BeanStream<T>.toMono8bitWav(uri: String): StreamOutput<T> =
        this.toWav<T, Unit, T>(uri, BitDepth.BIT_8, 1)

inline fun <reified T : Any> BeanStream<T>.toMono16bitWav(uri: String): StreamOutput<T> =
        this.toWav<T, Unit, T>(uri, BitDepth.BIT_16, 1)

inline fun <reified T : Any> BeanStream<T>.toMono24bitWav(uri: String): StreamOutput<T> =
        this.toWav<T, Unit, T>(uri, BitDepth.BIT_24, 1)

inline fun <reified T : Any> BeanStream<T>.toMono32bitWav(uri: String): StreamOutput<T> =
        this.toWav<T, Unit, T>(uri, BitDepth.BIT_32, 1)

inline fun <A : Any, reified T : Any> BeanStream<Managed<OutputSignal, A, T>>.toMono8bitWav(
        uri: String,
        noinline suffix: (A?) -> String
): StreamOutput<Managed<OutputSignal, A, T>> = toWav<Managed<OutputSignal, A, T>, A, T>(uri, BitDepth.BIT_8, 1, suffix)

inline fun <A : Any, reified T : Any> BeanStream<Managed<OutputSignal, A, T>>.toMono16bitWav(
        uri: String,
        noinline suffix: (A?) -> String
): StreamOutput<Managed<OutputSignal, A, T>> = toWav<Managed<OutputSignal, A, T>, A, T>(uri, BitDepth.BIT_16, 1, suffix)

inline fun <A : Any, reified T : Any> BeanStream<Managed<OutputSignal, A, T>>.toMono24bitWav(
        uri: String,
        noinline suffix: (A?) -> String
): StreamOutput<Managed<OutputSignal, A, T>> = toWav<Managed<OutputSignal, A, T>, A, T>(uri, BitDepth.BIT_24, 1, suffix)

inline fun <A : Any, reified T : Any> BeanStream<Managed<OutputSignal, A, T>>.toMono32bitWav(
        uri: String,
        noinline suffix: (A?) -> String
): StreamOutput<Managed<OutputSignal, A, T>> = toWav<Managed<OutputSignal, A, T>, A, T>(uri, BitDepth.BIT_32, 1, suffix)

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


class WavOutputException(message: String, cause: Exception? = null) : Exception(message, cause)

@Serializable
data class WavFileOutputParams<A : Any>(
        val uri: String,
        val bitDepth: BitDepth,
        val numberOfChannels: Int,
        val suffix: Fn<A?, String> = Fn.wrap { "" },
) : BeanParams()

class WavFileOutput(
        val stream: BeanStream<Any>,
        val params: WavFileOutputParams<Unit>
) : StreamOutput<Any>, SinglePartitionBean {

    override val input: Bean<Any> = stream

    override val parameters: BeanParams = params

    override fun writer(sampleRate: Float): Writer =
            AnyWavWriter(
                    stream,
                    params.bitDepth,
                    sampleRate,
                    params.numberOfChannels,
                    plainFileWriterDelegate<Unit>(params.uri),
                    WavFileOutput::class
            )

}

class WavPartialFileOutput<A : Any>(
        val stream: BeanStream<Managed<OutputSignal, A, Any>>,
        val params: WavFileOutputParams<A>
) : StreamOutput<Managed<OutputSignal, A, Any>>, SinglePartitionBean {

    override val input: Bean<Managed<OutputSignal, A, Any>> = stream

    override val parameters: BeanParams = params

    override fun writer(sampleRate: Float): Writer =
            AnyWavPartialWriter(
                    stream,
                    params.bitDepth,
                    sampleRate,
                    params.numberOfChannels,
                    suffixedFileWriterDelegate(params.uri) { params.suffix.apply(it) },
                    WavPartialFileOutput::class
            )

}

