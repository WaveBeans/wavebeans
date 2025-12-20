package io.wavebeans.lib.stream

import io.wavebeans.lib.BeanStream
import io.wavebeans.lib.Sample
import io.wavebeans.lib.executionScope

operator fun BeanStream<Sample>.times(multiplier: Number): BeanStream<Sample> = this.changeAmplitude(multiplier.toDouble())
operator fun BeanStream<Sample>.div(divisor: Number): BeanStream<Sample> = this.changeAmplitude(1.0 / divisor.toDouble())

fun BeanStream<Sample>.changeAmplitude(multiplier: Number): BeanStream<Sample> {
    return this.map(executionScope { add("multiplier", multiplier.toDouble()) }) {
        val m = parameters.double("multiplier")
        it * m
    }
}