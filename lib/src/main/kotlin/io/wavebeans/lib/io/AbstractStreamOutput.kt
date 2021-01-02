package io.wavebeans.lib.io

import io.wavebeans.lib.BeanStream
import mu.KotlinLogging

/**
 * The base implementation for the output beans. It requests correctly the desired sample rate and makes sure it
 * corresponds to the sample rate it is asked to process the stream in. If that's succeeded, initializes the input
 * sequence and creates the [Writer].
 *
 * @param I the type of the sample it digests
 */
abstract class AbstractStreamOutput<I : Any>(
        override val input: BeanStream<I>
) : StreamOutput<I> {

    companion object {
        private val log = KotlinLogging.logger { }
    }

    final override fun writer(sampleRate: Float): Writer {
        val desiredSampleRate = input.desiredSampleRate
        require(desiredSampleRate == null || desiredSampleRate == sampleRate) {
            "[$this] The stream should be resampled from ${desiredSampleRate}Hz to ${sampleRate}Hz before writing"
        }
        val inputSequence = input.asSequence(sampleRate)
        log.trace { "[$this] Initialized writer with input sample rate ${sampleRate}Hz" }
        return outputWriter(inputSequence, sampleRate)
    }

    /**
     * Creates the actual writer of the sequence.
     *
     * @param inputSequence the sequence to read from and write.
     * @param sampleRate the sample rate the output is sampled in, it corresponds with input desired sample rate and as was requested.
     */
    abstract fun outputWriter(inputSequence: Sequence<I>, sampleRate: Float): Writer
}