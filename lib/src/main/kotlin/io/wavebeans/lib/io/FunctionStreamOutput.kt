package io.wavebeans.lib.io

import io.wavebeans.lib.*
import io.wavebeans.metrics.clazzTag
import io.wavebeans.metrics.samplesProcessedOnOutputMetric
import mu.KotlinLogging
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName

inline fun <reified T : Any> BeanStream<T>.out(
        writeFunction: Fn<WriteFunctionArgument<T>, Boolean>
): StreamOutput<T> = FunctionStreamOutput(this, FunctionStreamOutputParams(T::class, writeFunction))

inline fun <reified T : Any> BeanStream<T>.out(
        noinline writeFunction: (WriteFunctionArgument<T>) -> Boolean
): StreamOutput<T> = this.out(Fn.wrap(writeFunction))

data class WriteFunctionArgument<T : Any>(
        val sampleClazz: KClass<T>,
        val sampleIndex: Long,
        val sampleRate: Float,
        val sample: T?,
        val phase: WriteFunctionPhase
)

enum class WriteFunctionPhase {
    WRITE,
    CLOSE
}

data class FunctionStreamOutputParams<T : Any>(
        val sampleClazz: KClass<T>,
        val writeFunction: Fn<WriteFunctionArgument<T>, Boolean>
) : BeanParams()

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
                    false
                }
            }

            override fun close() {
                log.debug { "Written $sampleCounter samples" }
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
