package io.wavebeans.lib.io

import io.github.oshai.kotlinlogging.KotlinLogging
import io.wavebeans.lib.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.*
import kotlin.reflect.KClass

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
    scope: ExecutionScope,
    noinline writeFunction: ExecutionScope.(WriteFunctionArgument<T>) -> Boolean
): StreamOutput<T> = FunctionStreamOutput(this, FunctionStreamOutputParams(T::class, scope, writeFunction))

@Deprecated(
    message = "Use out with lambda instead",
    replaceWith = ReplaceWith("out { it }")
)
inline fun <reified T : Any> BeanStream<T>.out(
    writeFunction: Fn<WriteFunctionArgument<T>, Boolean>
): StreamOutput<T> = this.out(EmptyScope) { writeFunction.apply(it) }

inline fun <reified T : Any> BeanStream<T>.out(
    noinline writeFunction: (WriteFunctionArgument<T>) -> Boolean
): StreamOutput<T> = this.out(EmptyScope) { writeFunction(it) }

/**
 * The argument of the output as a function routine.
 * * [sampleClazz] -- the class of the sample for convenience.
 * * [sampleIndex] -- the 0-based global index of the sample.
 * * [sampleRate] -- the sample rate the output is being evaluated with.
 * * [sample] -- the nullable depending on the phase `phase` sample value.
 * * [phase] -- the phase [WriteFunctionPhase] of the writing routine. Phase describes where is the writer currently is.
 */
//@Serializable
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
data class FunctionStreamOutputParams<T : Any>(
    /**
     * The class of the sample.
     */
    val sampleClazz: KClass<T>,
    /**
     * The execution scope.
     */
    val scope: ExecutionScope,
    /**
     * The function to invoke, has [WriteFunctionArgument] as an argument. Return the value of `Boolean`
     * type, that controls the output writer behavior:
     *  * In the [WriteFunctionPhase.WRITE] phase if the function returns `true` the writer will continue processing the input,
     *    if it returns `false` the writer will stop processing, but anyway [WriteFunctionPhase.CLOSE] phase will be initiated.
     *  * It doesn't affect anything in other phases.
     */
    val writeFunction: ExecutionScope.(WriteFunctionArgument<T>) -> Boolean
) : BeanParams

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
//        val samplesProcessed = samplesProcessedOnOutputMetric.withTags(clazzTag to FunctionStreamOutput::class.jvmName)
        var sampleCounter = 0L
        return object : Writer {
            override fun write(): Boolean {
                return if (sampleIterator.hasNext()) {
                    val sample = sampleIterator.next()
                    if (!parameters.writeFunction.invoke(
                            parameters.scope,
                            WriteFunctionArgument(
                                parameters.sampleClazz,
                                sampleCounter,
                                sampleRate,
                                sample,
                                WriteFunctionPhase.WRITE
                            )
                        )
                    ) return false
                    sampleCounter++
//                    samplesProcessed.increment()
                    true
                } else {
                    parameters.writeFunction.invoke(
                        parameters.scope,
                        WriteFunctionArgument(
                            parameters.sampleClazz,
                            sampleCounter,
                            sampleRate,
                            null,
                            WriteFunctionPhase.END
                        )
                    )
                    false
                }
            }

            override fun close() {
                log.debug { "Closing. Written $sampleCounter samples" }
                parameters.writeFunction.invoke(
                    parameters.scope,
                    WriteFunctionArgument(
                        parameters.sampleClazz,
                        sampleCounter,
                        sampleRate,
                        null,
                        WriteFunctionPhase.CLOSE
                    )
                )
            }
        }
    }
}
