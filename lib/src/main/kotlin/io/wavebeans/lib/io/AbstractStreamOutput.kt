package io.wavebeans.lib.io

import io.wavebeans.lib.BeanStream
import mu.KotlinLogging

abstract class AbstractStreamOutput<O : Any>(
        override val input: BeanStream<O>
) : StreamOutput<O> {

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

    abstract fun outputWriter(inputSequence: Sequence<O>, sampleRate: Float): Writer
}