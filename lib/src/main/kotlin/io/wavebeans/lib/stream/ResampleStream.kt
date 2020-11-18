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
import kotlin.math.truncate
import kotlin.reflect.jvm.jvmName

@JvmName("resampleSampleStream")
fun BeanStream<Sample>.resample(
        to: Float? = null,
        resampleFn: (ResamplingArgument<Sample>) -> Sequence<Sample>
): BeanStream<Sample> {
    return this.resample(to, Fn.wrap(resampleFn))
}

@JvmName("resampleSampleStream")
fun BeanStream<Sample>.resample(
        to: Float? = null,
        resampleFn: Fn<ResamplingArgument<Sample>, Sequence<Sample>> = SimpleResampleFn { it.average() }
): BeanStream<Sample> {
    return ResampleStream(this, ResampleStreamParams(to, resampleFn))
}

fun <T : Any> BeanStream<T>.resample(
        to: Float? = null,
        resampleFn: (ResamplingArgument<T>) -> Sequence<T>
): BeanStream<T> {
    return this.resample(to, Fn.wrap(resampleFn))
}

fun <T : Any> BeanStream<T>.resample(
        to: Float? = null,
        resampleFn: Fn<ResamplingArgument<T>, Sequence<T>> = SimpleResampleFn { throw IllegalStateException("reduce function is not defined") }
): BeanStream<T> {
    return ResampleStream(this, ResampleStreamParams(to, resampleFn))
}

@Serializable
data class ResamplingArgument<T>(
        val inputSampleRate: Float,
        val outputSampleRate: Float,
        val inputOutputFactor: Float,
        val inputSequence: Sequence<T>
)

class SimpleResampleFn<T : Any>(initParameters: FnInitParameters) : Fn<ResamplingArgument<T>, Sequence<T>>(initParameters) {

    constructor(reduceFn: Fn<List<T>, T>) : this(FnInitParameters().add("reduceFn", reduceFn))

    constructor(reduceFn: (List<T>) -> T) : this(FnInitParameters().add("reduceFn", wrap(reduceFn)))

    private val reduceFn: Fn<List<T>, T> by lazy { initParameters.fn<List<T>, T>("reduceFn") }

    override fun apply(argument: ResamplingArgument<T>): Sequence<T> {
        val reverseFactor = 1.0f / argument.inputOutputFactor

        return if (argument.inputOutputFactor == truncate(argument.inputOutputFactor) || reverseFactor == truncate(reverseFactor)) {
            when {
                argument.inputOutputFactor > 1 -> argument.inputSequence
                        .map { sample -> (0 until argument.inputOutputFactor.toInt()).asSequence().map { sample } }
                        .flatten()
                argument.inputOutputFactor < 1 -> argument.inputSequence
                        .windowed(reverseFactor.toInt(), reverseFactor.toInt(), partialWindows = true)
                        .map { samples -> reduceFn.apply(samples) }
                else -> argument.inputSequence
            }
        } else {
            TODO()
        }
    }
}

@Serializable(with = ResampleStreamParamsSerializer::class)
class ResampleStreamParams<T>(
        val to: Float?,
        val resampleFn: Fn<ResamplingArgument<T>, Sequence<T>>
) : BeanParams()

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