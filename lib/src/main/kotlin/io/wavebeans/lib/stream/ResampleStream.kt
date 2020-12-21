package io.wavebeans.lib.stream

import io.wavebeans.lib.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import mu.KotlinLogging
import kotlin.reflect.jvm.jvmName

/**
 * Resamples the stream of [Sample]s to match the output stream sample rate unless the [to] argument is specified explicitly.
 * The resampling is performed with the [resampleFn].
 *
 * @param to if specified used as a target sample rate, otherwise specified as a derivative from downstream output
 *           or another resample bean.
 * @param resampleFn the resampling function. Takes [ResamplingArgument] as an argument and returns [Sequence] of
 *        samples that are expected to be resampled to desired sample rate, and are treated accordingly.
 *
 * @return the stream that will be resampled to desired sample rate.
 */
@JvmName("resampleSampleStream")
fun BeanStream<Sample>.resample(
        to: Float? = null,
        resampleFn: (ResamplingArgument<Sample>) -> Sequence<Sample>
): BeanStream<Sample> {
    return this.resample(to, Fn.wrap(resampleFn))
}

/**
 * Resamples the stream of [Sample]s to match the output stream sample rate unless the [to] argument is specified explicitly.
 * The resampling is performed with the [resampleFn], where default implementation is [sincResampleFunc].
 *
 * @param to if specified used as a target sample rate, otherwise specified as a derivative from downstream output
 *        or another resample bean.
 * @param resampleFn the resampling function as instance of [Fn]. Takes [ResamplingArgument] as an argument and
 *        returns [Sequence] of samples that are expected to be resampled to desired sample rate, and are treated
 *        accordingly.
 *
 * @return the stream that will be resampled to desired sample rate.
 */
@JvmName("resampleSampleStream")
fun BeanStream<Sample>.resample(
        to: Float? = null,
        resampleFn: Fn<ResamplingArgument<Sample>, Sequence<Sample>> = sincResampleFunc()
): BeanStream<Sample> {
    return ResampleStream(this, ResampleStreamParams(to, resampleFn))
}

/**
 * Resamples the stream of type [T] to match the output stream sample rate unless the [to] argument is specified explicitly.
 * The resampling is performed with the [resampleFn].
 *
 * @param to if specified used as a target sample rate, otherwise specified as a derivative from downstream output
 *        or another resample bean.
 * @param resampleFn the resampling function. Takes [ResamplingArgument] as an argument and returns [Sequence] of samples
 *        that are expected to be resampled to desired sample rate, and are treated accordingly.
 * @param T the type of the sample being processed.
 *
 * @return the stream that will be resampled to desired sample rate.
 */
fun <T : Any> BeanStream<T>.resample(
        to: Float? = null,
        resampleFn: (ResamplingArgument<T>) -> Sequence<T>
): BeanStream<T> {
    return this.resample(to, Fn.wrap(resampleFn))
}

/**
 * Resamples the stream of type [T] to match the output stream sample rate unless the [to] argument is specified explicitly.
 * The resampling is performed with the [resampleFn], where default implementation is [SimpleResampleFn] without
 * [SimpleResampleFn.reduceFn].
 *
 * @param to if specified used as a target sample rate, otherwise specified as a derivative from downstream output
 *        or another resample bean.
 * @param resampleFn the resampling function as instance of [Fn]. Takes [ResamplingArgument] as an argument and
 *        returns [Sequence] of samples that are expected to be resampled to desired sample rate, and are treated
 *        accordingly.
 * @param T the type of the sample being processed.
 *
 * @return the stream that will be resampled to desired sample rate.
 */
fun <T : Any> BeanStream<T>.resample(
        to: Float? = null,
        resampleFn: Fn<ResamplingArgument<T>, Sequence<T>> = SimpleResampleFn()
): BeanStream<T> {
    return ResampleStream(this, ResampleStreamParams(to, resampleFn))
}

/**
 * The argument of the resampling function:
 * * [inputSampleRate] - the sample rate of the input stream.
 * * [outputSampleRate] - the desired sample rate of the output stream.
 * * [resamplingFactor] - the sample rate scale factor defined as input sample rate value divided by output sample rate value.
 * * [inputSequence] - the input stream as a sequence of elements of type [T].
 *
 * @param T the type of the sample being processed.
 */
@Serializable
data class ResamplingArgument<T>(
        /** The sample rate of the input stream. */
        val inputSampleRate: Float,
        /** The desired sample rate of the output stream. */
        val outputSampleRate: Float,
        /** The sample rate scale factor defined as input sample rate value divided by output sample rate value. */
        val resamplingFactor: Float,
        /** the input stream as a sequence of elements of type [T]. */
        val inputSequence: Sequence<T>
)

/**
 * Parameters for [ResampleStream]:
 * * [to] - if specified used as a target sample rate, otherwise specified as a derivative from downstream output
 *        or another resample bean.
 * * [resampleFn] - the resampling function as instance of [Fn]. Takes [ResamplingArgument] as an argument and
 *        returns [Sequence] of samples that are expected to be resampled to desired sample rate, and are treated
 *        accordingly.
 *
 * @param T the type of the sample being processed.
 */
@Serializable(with = ResampleStreamParamsSerializer::class)
class ResampleStreamParams<T>(
        /**
         * If specified used as a target sample rate, otherwise specified as a derivative from downstream output
         * or another resample bean.
         */
        val to: Float?,
        /**
         * The resampling function as instance of [Fn]. Takes [ResamplingArgument] as an argument and
         * returns [Sequence] of samples that are expected to be resampled to desired sample rate, and are treated
         * accordingly.
         */
        val resampleFn: Fn<ResamplingArgument<T>, Sequence<T>>
) : BeanParams()

/**
 * Serializer for [ResampleStreamParams].
 */
object ResampleStreamParamsSerializer : KSerializer<ResampleStreamParams<*>> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(ResampleStreamParamsSerializer::class.jvmName) {
        element("to", Float.serializer().nullable.descriptor)
        element("resampleFn", FnSerializer.descriptor)
    }

    override fun deserialize(decoder: Decoder): ResampleStreamParams<*> {
        val dec = decoder.beginStructure(descriptor)
        var to: Float? = null
        var resampleFn: Fn<ResamplingArgument<Any>, Sequence<Any>>? = null
        @Suppress("UNCHECKED_CAST")
        loop@ while (true) {
            when (val i = dec.decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break@loop
                0 -> to = dec.decodeNullableSerializableElement(descriptor, i, Float.serializer().nullable)
                1 -> resampleFn = dec.decodeSerializableElement(descriptor, i, FnSerializer) as Fn<ResamplingArgument<Any>, Sequence<Any>>
                else -> throw SerializationException("Unknown index $i")
            }
        }
        return ResampleStreamParams(to, resampleFn!!)
    }

    override fun serialize(encoder: Encoder, value: ResampleStreamParams<*>) {
        val structure = encoder.beginStructure(descriptor)
        structure.encodeNullableSerializableElement(descriptor, 0, Float.serializer(), value.to)
        structure.encodeSerializableElement(descriptor, 1, FnSerializer, value.resampleFn)
        structure.endStructure(descriptor)
    }
}

/**
 * Resamples the stream of type [T] to match the output stream sample rate unless the [ResampleStreamParams.to] argument is specified explicitly.
 * The resampling is performed with the [ResampleStreamParams.resampleFn].
 *
 * @param T the type of the sample being processed.
 */
class ResampleStream<T : Any>(
        override val input: BeanStream<T>,
        override val parameters: ResampleStreamParams<T>,
) : BeanStream<T>, SingleBean<T>, SinglePartitionBean {

    companion object {
        private val log = KotlinLogging.logger { }
    }

    override val desiredSampleRate: Float? = parameters.to

    override fun asSequence(sampleRate: Float): Sequence<T> {
        val ifs = input.desiredSampleRate ?: sampleRate
        val ofs = parameters.to ?: sampleRate
        val sequence = input.asSequence(ifs)
        val factor = ofs / ifs
        val argument = ResamplingArgument(ifs, ofs, factor, sequence)
        log.trace { "Initialized resampling from ${ifs}Hz to ${ofs}Hz [$argument]" }
        return parameters.resampleFn.apply(argument)
    }
}