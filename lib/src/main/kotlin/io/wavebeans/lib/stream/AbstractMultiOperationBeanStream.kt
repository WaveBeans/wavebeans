package io.wavebeans.lib.stream

import io.wavebeans.lib.BeanStream
import mu.KotlinLogging

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

    abstract fun operationSequence(inputs: List<Sequence<Any>>, sampleRate: Float): Sequence<O>
}