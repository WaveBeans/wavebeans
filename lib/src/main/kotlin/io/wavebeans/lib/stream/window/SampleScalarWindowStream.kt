package io.wavebeans.lib.stream.window

import io.wavebeans.lib.*
import io.wavebeans.lib.stream.map

operator fun BeanStream<Window<Sample>>.minus(d: Number): BeanStream<Window<Sample>> =
        this.map(ScalarSampleWindowOpFn(d.toDouble(), "-"))

operator fun BeanStream<Window<Sample>>.plus(d: Number): BeanStream<Window<Sample>> =
        this.map(ScalarSampleWindowOpFn(d.toDouble(), "+"))

operator fun BeanStream<Window<Sample>>.times(d: Number): BeanStream<Window<Sample>> =
        this.map(ScalarSampleWindowOpFn(d.toDouble(), "*"))

operator fun BeanStream<Window<Sample>>.div(d: Number): BeanStream<Window<Sample>> =
        this.map(ScalarSampleWindowOpFn(d.toDouble(), "/"))

class ScalarSampleWindowOpFn(factor: Double, operator: String) : Fn<Window<Sample>, Window<Sample>>(
        FnInitParameters()
                .add("factor", factor.toString())
                .add("operator", operator)
) {
    override fun apply(argument: Window<Sample>): Window<Sample> {
        val d = initParams["factor"]?.toDouble()!!
        return when (initParams["operator"]) {
            "/" -> Window.ofSamples(argument.size, argument.step, argument.elements.map { it / d })
            "*" -> Window.ofSamples(argument.size, argument.step, argument.elements.map { it * d })
            "+" -> Window.ofSamples(argument.size, argument.step, argument.elements.map { it + d })
            "-" -> Window.ofSamples(argument.size, argument.step, argument.elements.map { it - d })
            else -> throw UnsupportedOperationException("Operator ${initParams["operator"]} is not supported")
        }
    }
}