package io.wavebeans.metrics.collector

data class GaugeAccumulator(
        val isSet: Boolean,
        val increment: Double
) {
    operator fun plus(another: GaugeAccumulator): GaugeAccumulator {
        return when {
            another.isSet -> another
            isSet -> GaugeAccumulator(true, increment + another.increment)
            else -> GaugeAccumulator(false, increment + another.increment)
        }
    }
}