package io.wavebeans.lib.stream.window

import io.wavebeans.lib.*
import io.wavebeans.lib.stream.map

private const val operandParamName = "operand"
private const val operatorParamName = "operator"

operator fun BeanStream<Window<Sample>>.minus(d: Number): BeanStream<Window<Sample>> {
    val operand = d.toDouble()
    return this.map(
        executionScope { add(operandParamName, operand).add(operatorParamName, "-") }
    ) { window ->
        val factor = parameters.double("operand")
        val operator = parameters.string("operator")
        ScalarSampleWindowOp(factor, operator).apply(window)
    }
}

operator fun BeanStream<Window<Sample>>.plus(d: Number): BeanStream<Window<Sample>> {
    val operand = d.toDouble()
    return this.map(
        executionScope { add(operandParamName, operand).add(operatorParamName, "+") }
    ) { window ->
        val factor = parameters.double("operand")
        val operator = parameters.string("operator")
        ScalarSampleWindowOp(factor, operator).apply(window)
    }
}

operator fun BeanStream<Window<Sample>>.times(d: Number): BeanStream<Window<Sample>> {
    val operand = d.toDouble()
    return this.map(
        executionScope { add(operandParamName, operand).add(operatorParamName, "*") }
    ) { window ->
        val factor = parameters.double("operand")
        val operator = parameters.string("operator")
        ScalarSampleWindowOp(factor, operator).apply(window)
    }
}

operator fun BeanStream<Window<Sample>>.div(d: Number): BeanStream<Window<Sample>> {
    val operand = d.toDouble()
    return this.map(
        executionScope { add(operandParamName, operand).add(operatorParamName, "/") }
    ) { window ->
        val factor = parameters.double(operandParamName)
        val operator = parameters.string("operator")
        ScalarSampleWindowOp(factor, operator).apply(window)
    }

}

private class ScalarSampleWindowOp(
    private val factor: Double,
    private val operator: String
) {

    fun apply(argument: Window<Sample>): Window<Sample> {
        val d = factor
        return when (operator) {
            "/" -> Window.ofSamples(argument.size, argument.step, argument.elements.map { it / d })
            "*" -> Window.ofSamples(argument.size, argument.step, argument.elements.map { it * d })
            "+" -> Window.ofSamples(argument.size, argument.step, argument.elements.map { it + d })
            "-" -> Window.ofSamples(argument.size, argument.step, argument.elements.map { it - d })
            else -> throw UnsupportedOperationException("Operator $operator is not supported")
        }
    }
}