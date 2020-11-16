package io.wavebeans.lib.io

import io.wavebeans.lib.BeanStream
import mu.KotlinLogging

abstract class AbstractInputBeanStream<O : Any> : BeanStream<O> {

    companion object {
        private val log = KotlinLogging.logger { }
    }

    final override fun asSequence(sampleRate: Float): Sequence<O> {
        require(desiredSampleRate == null || desiredSampleRate == sampleRate) { "The stream should be resampled from ${desiredSampleRate}Hz to ${sampleRate}Hz" }
        val fs = desiredSampleRate ?: sampleRate
        log.trace { "[$this] Initialized the input with sample rate ${fs}Hz while desired is $desiredSampleRate" }
        return inputSequence(fs)
    }

    protected abstract fun inputSequence(sampleRate: Float): Sequence<O>
}