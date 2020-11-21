package io.wavebeans.lib.stream

import io.wavebeans.lib.BeanStream
import io.wavebeans.lib.io.Writer
import mu.KotlinLogging

/**
 * The base implementation for the operation beans with one input. It requests correctly the desired sample rate from
 * the input and bypasses it to the next operation. Initializes the sequence with the desired sample rate if that was
 * specified of with requested sample rate.
 *
 * @param I the type of the sample it digests.
 * @param O the type of the sample it produces.
 */
abstract class AbstractOperationBeanStream<I : Any, O : Any>(
        val inputBean: BeanStream<I>
) : BeanStream<O> {

    companion object {
        private val log = KotlinLogging.logger { }
    }

    override val desiredSampleRate: Float? by lazy { inputBean.desiredSampleRate }

    final override fun asSequence(sampleRate: Float): Sequence<O> {
        val ofs = desiredSampleRate ?: sampleRate
        val sequence = inputBean.asSequence(ofs)
        log.trace { "[$this] Initialized operation sequence with sample rate ${ofs}Hz" }
        return operationSequence(sequence, ofs)
    }

    /**
     * Creates the actual processing sequence operation.
     *
     * @param input the sequence to read from.
     * @param sampleRate the sample rate the output is sampled in, it corresponds with input desired sample rate or as was requested.
     *
     * @return the processed sequence.
    */
    abstract fun operationSequence(input: Sequence<I>, sampleRate: Float): Sequence<O>
}
