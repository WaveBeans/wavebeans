package io.wavebeans.lib.stream

import io.wavebeans.lib.BeanStream
import mu.KotlinLogging

abstract class AbstractOperationBeanStream<I : Any, O : Any>(
        val inputs: List<BeanStream<I>>
) : BeanStream<O> {

    companion object {
        private val log = KotlinLogging.logger { }
    }

    override var outputSampleRate: Float? = null
        protected set

    override var inputSampleRate: Float? = null
        protected set

    override fun asSequence(sampleRate: Float): Sequence<O> {
        val sequences = inputs.map { it.asSequence(sampleRate) }
        inputSampleRate = inputs.first().outputSampleRate
        require(inputs.all { it.outputSampleRate == inputSampleRate }) { "All input must be resample beforehand to ${inputSampleRate}Hz" }
        outputSampleRate = inputSampleRate
        val ofs = inputSampleRate ?: sampleRate
        log.trace { "Initialized operation sequence ${this::class} with output sample rate ${ofs}Hz" }
        return operationSequence(sequences, ofs)
    }

    abstract fun operationSequence(inputs: List<Sequence<I>>, sampleRate: Float): Sequence<O>
}