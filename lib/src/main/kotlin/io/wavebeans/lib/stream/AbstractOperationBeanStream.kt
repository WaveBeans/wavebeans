package io.wavebeans.lib.stream

import io.wavebeans.lib.BeanStream
import mu.KotlinLogging

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

    abstract fun operationSequence(input: Sequence<I>, sampleRate: Float): Sequence<O>
}
