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
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.jvm.jvmName
import kotlin.reflect.typeOf

/**
 * Resamples the stream of [Sample]s to match the output stream sample rate unless the [to] argument is specified explicitly.
 * The resampling is performed with the [resampleFn].
 * If the sampling rate is not changed the resampling function is not called at all.
 *
 * @param to if specified used as a target sample rate, otherwise specified as a derivative from downstream output
 *           or another resample bean.
 * @param resampleFn the resampling function. Takes [ResamplingArgument] as an argument and returns [Sequence] of
 *        samples that are expected to be resampled to desired sample rate, and are treated accordingly.
 *
 * @return the stream that will be resampled to desired sample rate.
 */
@JvmName("resampleSampleStream")
inline fun <reified S : BeanStream<Sample>> S.resample(
        to: Float? = null,
        noinline resampleFn: (ResamplingArgument<Sample>) -> Sequence<Sample>,
): S {
    return this.resample(to, Fn.wrap(resampleFn))
}

/**
 * Resamples the stream of [Sample]s to match the output stream sample rate unless the [to] argument is specified explicitly.
 * The resampling is performed with the [resampleFn], where default implementation is [sincResampleFunc].
 * If the sampling rate is not changed the resampling function is not called at all.
 *
 * @param to if specified used as a target sample rate, otherwise specified as a derivative from downstream output
 *        or another resample bean.
 * @param resampleFn the resampling function as instance of [Fn]. Takes [ResamplingArgument] as an argument and
 *        returns [Sequence] of samples that are expected to be resampled to desired sample rate, and are treated
 *        accordingly.
 *
 * @return the stream that will be resampled to desired sample rate.
 */
@Suppress("UNCHECKED_CAST")
@JvmName("resampleSampleStream")
inline fun <reified S : BeanStream<Sample>> S.resample(
        to: Float? = null,
        resampleFn: Fn<ResamplingArgument<Sample>, Sequence<Sample>> = sincResampleFunc(),
): S {
    return when (val streamType = typeOf<S>()) {
        typeOf<BeanStream<Sample>>() ->
            ResampleBeanStream(this, ResampleStreamParams(to, resampleFn)) as S
        typeOf<FiniteStream<Sample>>() ->
            ResampleFiniteStream(this as FiniteStream<Sample>, ResampleStreamParams(to, resampleFn)) as S
        else -> throw UnsupportedOperationException("Type $streamType is not supported for resampling")
    }

}

/**
 * Resamples the stream of type [T] to match the output stream sample rate unless the [to] argument is specified explicitly.
 * The resampling is performed with the [resampleFn].
 * If the sampling rate is not changed the resampling function is not called at all.
 *
 * @param to if specified used as a target sample rate, otherwise specified as a derivative from downstream output
 *        or another resample bean.
 * @param resampleFn the resampling function. Takes [ResamplingArgument] as an argument and returns [Sequence] of samples
 *        that are expected to be resampled to desired sample rate, and are treated accordingly.
 * @param T the type of the sample being processed.
 *
 * @return the stream that will be resampled to desired sample rate.
 */
inline fun <reified S: BeanStream<T>, T : Any> S.resample(
        to: Float? = null,
        noinline resampleFn: (ResamplingArgument<T>) -> Sequence<T>,
): S {
    return this.resample(to, Fn.wrap(resampleFn))
}

/**
 * Resamples the stream of type [T] to match the output stream sample rate unless the [to] argument is specified explicitly.
 * The resampling is performed with the [resampleFn], where default implementation is [SimpleResampleFn] without
 * [SimpleResampleFn.reduceFn].
 * If the sampling rate is not changed the resampling function is not called at all.
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
@Suppress("UNCHECKED_CAST")
inline fun <reified S: BeanStream<T>, T : Any> S.resample(
        to: Float? = null,
        resampleFn: Fn<ResamplingArgument<T>, Sequence<T>> = SimpleResampleFn(),
): S {
    val streamType = typeOf<S>()
    return when {
        streamType.isSubtypeOf(typeOf<BeanStream<*>>()) ->
            ResampleBeanStream(this, ResampleStreamParams(to, resampleFn)) as S
        streamType.isSubtypeOf(typeOf<FiniteStream<*>>()) ->
            ResampleFiniteStream(this as FiniteStream<T>, ResampleStreamParams(to, resampleFn)) as S
        else -> throw UnsupportedOperationException("Type $streamType is not supported for resampling")
    }
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
        val inputSequence: Sequence<T>,
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
        val resampleFn: Fn<ResamplingArgument<T>, Sequence<T>>,
) : BeanParams

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
 * Resamples the infinite stream of type [T] to match the output stream sample rate unless the [ResampleStreamParams.to]
 * argument is specified explicitly. The resampling is performed with the [ResampleStreamParams.resampleFn].
 * If the sampling rate is not changed the resampling function is not called at all.
 *
 * @param T the type of the sample being processed.
 */
class ResampleBeanStream<T : Any>(
        input: BeanStream<T>,
        parameters: ResampleStreamParams<T>,
) : AbstractResampleStream<T>(input, parameters), BeanStream<T> {

    override val desiredSampleRate: Float?
        get() = beanDesiredSampleRate

    override fun asSequence(sampleRate: Float): Sequence<T> = generateSequence(sampleRate)
}

/**
 * Resamples the finite stream of type [T] to match the output stream sample rate unless the [ResampleStreamParams.to]
 * argument is specified explicitly. The resampling is performed with the [ResampleStreamParams.resampleFn].
 * If the sampling rate is not changed the resampling function is not called at all.
 *
 * @param T the type of the sample being processed.
 */
class ResampleFiniteStream<T : Any>(
        override val input: FiniteStream<T>,
        parameters: ResampleStreamParams<T>,
) : AbstractResampleStream<T>(input, parameters), FiniteStream<T> {

    private var sampleRate by Delegates.notNull<Float>()

    override val desiredSampleRate: Float?
        get() = beanDesiredSampleRate

    override fun asSequence(sampleRate: Float): Sequence<T> {
        this.sampleRate = sampleRate
        return generateSequence(sampleRate)
    }

    override fun length(timeUnit: TimeUnit): Long = input.length()

    override fun samplesCount(): Long = (
            input.samplesCount() *
                    (input.desiredSampleRate ?: sampleRate) /
                    (parameters.to ?: sampleRate)
            ).toLong()
}

abstract class AbstractResampleStream<T : Any>(
        override val input: BeanStream<T>,
        final override val parameters: ResampleStreamParams<T>,
) : SingleBean<T>, SinglePartitionBean {

    companion object {
        private val log = KotlinLogging.logger { }
    }

    protected val beanDesiredSampleRate: Float? = parameters.to

    protected fun generateSequence(sampleRate: Float): Sequence<T> {
        val ifs = input.desiredSampleRate ?: sampleRate
        val ofs = parameters.to ?: sampleRate
        val sequence = input.asSequence(ifs)
        return if (ifs == ofs) {
            log.trace { "[$this] By-passing resampling as input sample rate (${ifs}Hz) == output sample rate (${ofs}Hz) [input=$input, parameters=$parameters]" }
            sequence
        } else {
            val factor = ofs / ifs
            val argument = ResamplingArgument(ifs, ofs, factor, sequence)
            log.trace { "[$this] Initialized resampling from ${ifs}Hz to ${ofs}Hz ($argument) [input=$input, parameters=$parameters]" }
            parameters.resampleFn.apply(argument)
        }

    }
}