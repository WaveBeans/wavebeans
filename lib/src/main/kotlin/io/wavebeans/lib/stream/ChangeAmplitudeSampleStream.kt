package io.wavebeans.lib.stream

import io.wavebeans.lib.*
import kotlinx.serialization.Serializable

operator fun BeanStream<Sample>.times(multiplier: Number): BeanStream<Sample> = this.changeAmplitude(multiplier.toDouble())
operator fun BeanStream<Sample>.div(divisor: Number): BeanStream<Sample> = this.changeAmplitude(1.0 / divisor.toDouble())

class ChangeAmplitudeFn(initParameters: FnInitParameters) : Fn<Sample, Sample>(initParameters) {

    constructor(multiplier: Number) : this(FnInitParameters().add("multiplier", multiplier.toDouble()))

    private val multiplier by lazy { initParameters.double("multiplier") }

    override fun apply(argument: Sample): Sample = argument * multiplier
}

fun BeanStream<Sample>.changeAmplitude(multiplier: Number): BeanStream<Sample> = this.map(ChangeAmplitudeFn(multiplier))