package io.wavebeans.lib.io

import io.wavebeans.lib.*
import io.wavebeans.metrics.clazzTag
import io.wavebeans.metrics.samplesProcessedOnOutputMetric
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import mu.KotlinLogging
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName

/**
 * Output as a function. Invokes the specified function on each sample during writing via [Writer].
 *
 * @param writeFunction the function to invoke, has [WriteFunctionArgument] as an argument.
 *
 * @return the value of `Boolean` type, that controls the output writer behavior:
 * * In the [WriteFunctionPhase.WRITE] phase if the function returns `true` the writer will continue processing the input,
 *   if it returns `false` the writer will stop processing, but anyway [WriteFunctionPhase.CLOSE] phase will be initiated.
 * * It doesn't affect anything in other phases.
 */
inline fun <reified T : Any> BeanStream<T>.out(
        writeFunction: Fn<WriteFunctionArgument<T>, Boolean>
): StreamOutput<T> = FunctionStreamOutput(this, FunctionStreamOutputParams(T::class, writeFunction))

/**
 * Output as a function. Invokes the specified function on each sample during writing via [Writer].
 *
 * @param writeFunction the function as [Fn] to invoke, has [WriteFunctionArgument] as an argument.
 *
 * @return the value of `Boolean` type, that controls the output writer behavior:
 * * In the [WriteFunctionPhase.WRITE] phase if the function returns `true` the writer will continue processing the input,
 *   if it returns `false` the writer will stop processing, but anyway [WriteFunctionPhase.CLOSE] phase will be initiated.
 * * It doesn't affect anything in other phases.
 */
inline fun <reified T : Any> BeanStream<T>.out(
        noinline writeFunction: (WriteFunctionArgument<T>) -> Boolean
): StreamOutput<T> = this.out(Fn.wrap(writeFunction))

/**
 * The argument of the output as a function routine.
 * * [sampleClazz] -- the class of the sample for convenience.
 * * [sampleIndex] -- the 0-based global index of the sample.
 * * [sampleRate] -- the sample rate the output is being evaluated with.
 * * [sample] -- the nullable depending on the phase `phase` sample value.
 * * [phase] -- the phase [WriteFunctionPhase] of the writing routine. Phase describes where is the writer currently is.
 */
@Serializable
data class WriteFunctionArgument<T : Any>(
        val sampleClazz: KClass<T>,
        val sampleIndex: Long,
        val sampleRate: Float,
        val sample: T?,
        val phase: WriteFunctionPhase
)

/**
 * The phase of the writing routine.
 */
enum class WriteFunctionPhase {
    /**
     * Tells that the writer is currently getting the input signal, and expect it to process. The `sample` field
     * is never `null` in this case.
     */
    WRITE,
    /**
     * Tells that the writer has reached the end of the input stream, but the writer has been called. May not be called
     * in some cases (i.e. the writer's write function is stopped calling before the writer hit on the end of the stream,
     * or the stream is endless), or be called more than once (in case that the writer's write function is called after
     * the previous call returned `false`), but during regular execution is being called only once. The `sample` field
     * is `null` in this case.
     */
    END,
    /**
     * Tells that the writer is being closed. The `sample` field is `null` in this case.
     */
    CLOSE,
}

/**
 * Parameters for [FunctionStreamOutput].
 *
 * [sampleClazz] The class of the sample.
 *
 * [writeFunction] -- The function as [Fn] to invoke, has [WriteFunctionArgument] as an argument. Return the value of `Boolean`
 * type, that controls the output writer behavior:
 *  * In the [WriteFunctionPhase.WRITE] phase if the function returns `true` the writer will continue processing the input,
 *    if it returns `false` the writer will stop processing, but anyway [WriteFunctionPhase.CLOSE] phase will be initiated.
 *  * It doesn't affect anything in other phases.
 */
@Serializable(with = FunctionStreamOutputParamsSerializer::class)
data class FunctionStreamOutputParams<T : Any>(
        /**
         * The class of the sample.
         */
        val sampleClazz: KClass<T>,
        /**
         * The function as [Fn] to invoke, has [WriteFunctionArgument] as an argument. Return the value of `Boolean`
         * type, that controls the output writer behavior:
         *  * In the [WriteFunctionPhase.WRITE] phase if the function returns `true` the writer will continue processing the input,
         *    if it returns `false` the writer will stop processing, but anyway [WriteFunctionPhase.CLOSE] phase will be initiated.
         *  * It doesn't affect anything in other phases.
         */
        val writeFunction: Fn<WriteFunctionArgument<T>, Boolean>
) : BeanParams

/**
 * Serializer for [FunctionStreamOutputParams].
 */
object FunctionStreamOutputParamsSerializer : KSerializer<FunctionStreamOutputParams<*>> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(FunctionStreamOutputParams::class.jvmName) {
        element("sampleClazz", String.serializer().descriptor)
        element("writeFunction", FnSerializer.descriptor)
    }

    override fun deserialize(decoder: Decoder): FunctionStreamOutputParams<*> {
        val dec = decoder.beginStructure(descriptor)
        var sampleClazz: KClass<Any>? = null
        var writeFunction: Fn<WriteFunctionArgument<Any>, Boolean>? = null
        @Suppress("UNCHECKED_CAST")
        loop@ while (true) {
            when (val i = dec.decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break@loop
                0 -> sampleClazz = WaveBeansClassLoader.classForName(dec.decodeStringElement(descriptor, i)).kotlin as KClass<Any>
                1 -> writeFunction = dec.decodeSerializableElement(descriptor, i, FnSerializer) as Fn<WriteFunctionArgument<Any>, Boolean>
                else -> throw SerializationException("Unknown index $i")
            }
        }
        return FunctionStreamOutputParams(sampleClazz!!, writeFunction!!)
    }

    override fun serialize(encoder: Encoder, value: FunctionStreamOutputParams<*>) {
        val structure = encoder.beginStructure(descriptor)
        structure.encodeSerializableElement(descriptor, 0, String.serializer(), value.sampleClazz.jvmName)
        structure.encodeSerializableElement(descriptor, 1, FnSerializer, value.writeFunction)
        structure.endStructure(descriptor)
    }
}

/**
 * Output as a function. Invokes the specified function on each sample during writing via [Writer].
 *
 * @param input the stream to perform output from
 * @param parameters the tuning parameters as [FunctionStreamOutputParams].
 */
class FunctionStreamOutput<T : Any>(
        override val input: BeanStream<T>,
        override val parameters: FunctionStreamOutputParams<T>
) : AbstractStreamOutput<T>(input), SinglePartitionBean {

    companion object {
        private val log = KotlinLogging.logger { }
    }

    override fun outputWriter(inputSequence: Sequence<T>, sampleRate: Float): Writer {
        val sampleIterator = inputSequence.iterator()
        val samplesProcessed = samplesProcessedOnOutputMetric.withTags(clazzTag to FunctionStreamOutput::class.jvmName)
        var sampleCounter = 0L
        return object : Writer {
            override fun write(): Boolean {
                return if (sampleIterator.hasNext()) {
                    val sample = sampleIterator.next()
                    if (!parameters.writeFunction.apply(WriteFunctionArgument(
                                    parameters.sampleClazz,
                                    sampleCounter,
                                    sampleRate,
                                    sample,
                                    WriteFunctionPhase.WRITE
                            ))
                    ) return false
                    sampleCounter++
                    samplesProcessed.increment()
                    true
                } else {
                    parameters.writeFunction.apply(WriteFunctionArgument(
                            parameters.sampleClazz,
                            sampleCounter,
                            sampleRate,
                            null,
                            WriteFunctionPhase.END
                    ))
                    false
                }
            }

            override fun close() {
                log.debug { "Closing. Written $sampleCounter samples" }
                parameters.writeFunction.apply(WriteFunctionArgument(
                        parameters.sampleClazz,
                        sampleCounter,
                        sampleRate,
                        null,
                        WriteFunctionPhase.CLOSE
                ))
            }
        }
    }
}
