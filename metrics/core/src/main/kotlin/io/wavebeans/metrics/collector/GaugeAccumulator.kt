package io.wavebeans.metrics.collector

/**
 * Accumulate progess measures, the [isSet]=`true` represents that the value was reset and [increment] should
 * be applied on zero value but do not accumulate. If the [isSet]=`false` the value needs to be accumulated.
 */
data class GaugeAccumulator(
        val isSet: Boolean,
        val increment: Double
) {

    /**
     * Adds up the current value with the specified one according to rules:
     * * if another.[isSet] == `true`, then the value is being reset, hence the result is just `another` as is
     * * if this.[isSet] == `true`, then the accumulative value will also be reset value, but the increment is summed up of both operands.
     * * else the accumulative value is not the reset value ([isSet]=`false`) and the increment is summed up of both operands.
     *
     * **The operation is not commutative. The order of operands matters.**
     */
    operator fun plus(another: GaugeAccumulator): GaugeAccumulator {
        return when {
            another.isSet -> another
            isSet -> GaugeAccumulator(true, increment + another.increment)
            else -> GaugeAccumulator(false, increment + another.increment)
        }
    }
}