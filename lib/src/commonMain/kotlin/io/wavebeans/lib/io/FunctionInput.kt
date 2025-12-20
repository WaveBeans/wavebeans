package io.wavebeans.lib.io

import io.wavebeans.lib.*

/**
 * Creates an input from provided function. The function has two parameters: the 0-based index and sample rate the input
 * expected to be evaluated.
 *
 * @param generator generator function of two parameters: the 0-based index and sample rate the input
 *                  expected to be evaluated.
 */
fun <T : Any> input(generator: (Long, Float) -> T?): BeanStream<T> = Input(InputParams(generator))

/**
 * Creates an input from provided function. The function has two parameters: the 0-based index and sample rate the input
 * expected to be evaluated.
 *
 * @param sampleRate the sample rate that input supports.
 * @param generator generator function of two parameters: the 0-based index and sample rate the input
 *                  expected to be evaluated.
 */
fun <T : Any> inputWithSampleRate(sampleRate: Float, generator: (Long, Float) -> T?): BeanStream<T> =
    Input(InputParams(generator, sampleRate))

/**
 * Tuning parameters for [Input].
 *
 * [generator] is a function as [Fn] of two parameters: the 0-based index and sample rate the input expected to be evaluated.
 * [sampleRate] is the sample rate that input supports, or null if it'll automatically adapt.
 */
class InputParams<T : Any>(
    val generator: (Long, Float) -> T?,
    val sampleRate: Float? = null
) : BeanParams

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

//    private val samplesProcessed = samplesProcessedOnInputMetric.withTags(clazzTag to parameters.generator::class.jvmName)

    override val desiredSampleRate: Float? = parameters.sampleRate

    override fun inputSequence(sampleRate: Float): Sequence<T> {
        return (0..Long.MAX_VALUE).asSequence()
            .map { parameters.generator.invoke(it, sampleRate) }
            .takeWhile { it != null }
            .map { it!! }
//                .map { samplesProcessed.increment(); it!! }
    }
}