package io.wavebeans.lib.stream

import io.wavebeans.lib.BeanStream
import mu.KotlinLogging

/**
 * The base implementation for the operation beans with mmultiple inputs. It requests correctly the desired sample rate from
 * the inputs and bypasses it to the next operation. Expects all inputs to have the same sample rate.
 * Initializes the sequence with the desired sample rate if that was specified of with requested sample rate.
 *
 * @param O the type of the sample it produces.
 */
abstract class AbstractMultiOperationBeanStream<O : Any>(
        val inputBeans: List<BeanStream<out Any>>
) : BeanStream<O> {

    companion object {
        private val log = KotlinLogging.logger { }
    }

    override val desiredSampleRate: Float? by lazy {
        inputBeans.mapNotNull { it.desiredSampleRate }
                .distinct()
                .let {
                    require(it.size <= 1) { "[$this] Can't resolve sample rate of inputs, found $it among $inputBeans" }
                    if (it.isEmpty()) null
                    else it.first()
                }
    }

    final override fun asSequence(sampleRate: Float): Sequence<O> {
        val ofs = desiredSampleRate ?: sampleRate
        val sequences = inputBeans.map { it.asSequence(ofs) }
        log.trace { "[$this] Initialized operation sequence with sample rate ${ofs}Hz" }
        return operationSequence(sequences, ofs)
    }

    /**
     * Creates the actual processing sequence operation.
     *
     * @param inputs the sequences to read from.
     * @param sampleRate the sample rate the output is sampled in, it corresponds with input desired sample rate or as was requested.
     *
     * @return the processed sequence.
     */
    abstract fun operationSequence(inputs: List<Sequence<Any>>, sampleRate: Float): Sequence<O>
}