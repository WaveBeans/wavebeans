package io.wavebeans.lib.io

import io.wavebeans.lib.BeanStream
import io.wavebeans.lib.SourceBean
import mu.KotlinLogging

/**
 * The base implementation for the input beans. It bypasses correctly the desired sample rate and makes sure the
 * sequence is initiated according to it.
 *
 * @param O the type of the sample it outputs.
 */
abstract class AbstractInputBeanStream<O : Any> : BeanStream<O>, SourceBean<O> {

    companion object {
        private val log = KotlinLogging.logger { }
    }

    final override fun asSequence(sampleRate: Float): Sequence<O> {
        require(desiredSampleRate == null || desiredSampleRate == sampleRate) {
            "[$this] The stream should be resampled from ${desiredSampleRate}Hz to ${sampleRate}Hz"
        }
        log.trace { "[$this] Initialized the input with sample rate ${sampleRate}Hz while desired is $desiredSampleRate" }
        return inputSequence(sampleRate)
    }

    /**
     * Gets the sequence of elements of this input.
     *
     * @param sampleRate the sample rate the input should operate in, corresponds to [desiredSampleRate] unless it was null.
     *
     * @return the input sequence.
     */
    protected abstract fun inputSequence(sampleRate: Float): Sequence<O>
}