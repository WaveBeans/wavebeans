package io.wavebeans.lib.io

import io.wavebeans.lib.*
import io.wavebeans.metrics.clazzTag
import io.wavebeans.metrics.samplesProcessedOnInputMetric
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
import kotlin.reflect.jvm.jvmName

/**
 * Creates an input from provided function. The function has two parameters: the 0-based index and sample rate the input
 * expected to be evaluated.
 *
 * @param generator generator function of two parameters: the 0-based index and sample rate the input
 *                  expected to be evaluated.
 */
fun <T : Any> input(generator: (Pair<Long, Float>) -> T?): BeanStream<T> = input(Fn.wrap(generator))

/**
 * Creates an input from provided function. The function has two parameters: the 0-based index and sample rate the input
 * expected to be evaluated.
 *
 * @param generator generator function as [Fn] of two parameters: the 0-based index and sample rate the input
 *                  expected to be evaluated.
 */
fun <T : Any> input(generator: Fn<Pair<Long, Float>, T?>): BeanStream<T> = Input(InputParams(generator))

/**
 * Creates an input from provided function. The function has two parameters: the 0-based index and sample rate the input
 * expected to be evaluated.
 *
 * @param sampleRate the sample rate that input supports.
 * @param generator generator function of two parameters: the 0-based index and sample rate the input
 *                  expected to be evaluated.
 */
fun <T : Any> inputWithSampleRate(sampleRate: Float, generator: (Pair<Long, Float>) -> T?): BeanStream<T> = input(sampleRate, Fn.wrap(generator))

/**
 * Creates an input from provided function. The function has two parameters: the 0-based index and sample rate the input
 * expected to be evaluated.
 *
 * @param sampleRate the sample rate that input supports.
 * @param generator generator function as [Fn] of two parameters: the 0-based index and sample rate the input
 *                  expected to be evaluated.
 */
fun <T : Any> input(sampleRate: Float, generator: Fn<Pair<Long, Float>, T?>): BeanStream<T> = Input(InputParams(generator, sampleRate))

/**
 * Serializer for [InputParams]
 */
object InputParamsSerializer : KSerializer<InputParams<*>> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(InputParams::class.jvmName) {
        element("generateFn", FnSerializer.descriptor)
        element("sampleRate", Float.serializer().nullable.descriptor)
    }

    override fun deserialize(decoder: Decoder): InputParams<*> {
        val dec = decoder.beginStructure(descriptor)
        var sampleRate: Float? = null
        var func: Fn<Pair<Long, Float>, Any?>? = null
        @Suppress("UNCHECKED_CAST")
        loop@ while (true) {
            when (val i = dec.decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break@loop
                0 -> func = dec.decodeSerializableElement(descriptor, i, FnSerializer) as Fn<Pair<Long, Float>, Any?>
                1 -> sampleRate = dec.decodeNullableSerializableElement(descriptor, i, Float.serializer().nullable)
                else -> throw SerializationException("Unknown index $i")
            }
        }
        return InputParams(func!!, sampleRate)
    }

    override fun serialize(encoder: Encoder, value: InputParams<*>) {
        val structure = encoder.beginStructure(descriptor)
        structure.encodeSerializableElement(descriptor, 0, FnSerializer, value.generator)
        structure.encodeNullableSerializableElement(descriptor, 1, Float.serializer().nullable, value.sampleRate)
        structure.endStructure(descriptor)
    }

}

/**
 * Tuning parameters for [Input].
 *
 * [generator] is a function as [Fn] of two parameters: the 0-based index and sample rate the input expected to be evaluated.
 * [sampleRate] is the sample rate that input supports, or null if it'll automatically adapt.
 */
@Serializable(with = InputParamsSerializer::class)
class InputParams<T : Any>(
        val generator: Fn<Pair<Long, Float>, T?>,
        val sampleRate: Float? = null
) : BeanParams()

/**
 * Creates an input from provided function. The function has two parameters: the 0-based index and sample rate the input
 * expected to be evaluated.
 *
 * @param parameters the tuning parameters:
 *  * [InputParams.sampleRate] -- the sample rate that input supports.
 *  * [InputParams.generator] function as [Fn] of two parameters: the 0-based index and sample rate the input
 *                  expected to be evaluated.
*/
class Input<T : Any>(
        override val parameters: InputParams<T>
) : AbstractInputBeanStream<T>(), SourceBean<T>, SinglePartitionBean {

    private val samplesProcessed = samplesProcessedOnInputMetric.withTags(clazzTag to parameters.generator::class.jvmName)

    override val desiredSampleRate: Float? = parameters.sampleRate

    override fun inputSequence(sampleRate: Float): Sequence<T> {
        return (0..Long.MAX_VALUE).asSequence()
                .map { parameters.generator.apply(Pair(it, sampleRate)) }
                .takeWhile { it != null }
                .map { samplesProcessed.increment(); it!! }
    }
}