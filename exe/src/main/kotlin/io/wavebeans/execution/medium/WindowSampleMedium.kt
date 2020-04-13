package io.wavebeans.execution.medium

import io.wavebeans.lib.Sample
import io.wavebeans.lib.stream.window.Window

/**
 * Transferring type for array of [SampleMedium] to keep [Sample]s inside [Window] within primitive [Double] during transfer.
 */
class WindowSampleMedium(
        val windowSize: Int,
        val windowStep: Int,
        val samples: Array<DoubleArray>
) : Medium {

    companion object {
        /**
         * Creates [WindowSampleMedium] of the defined [size], initializes the array using provided [windowFn] function
         *
         * @param size the size of the [SampleMedium] to create. Must be 1 or greater
         * @param windowFn the function the initializes the [Window] of [Sample]s. The parameter of the function is 0-based index of the element i
         */
        fun create(list: List<Window<Sample>>): WindowSampleMedium {
            val size = list.size
            require(size >= 1) { "Can't create sample array of the size $size" }
            val first = list.first()
            val windowSize = first.size
            val windowStep = first.step
            val i = list.iterator()
            val samples = Array(size) {
                val window = i.next()
                val iterator = window.elements.iterator()
                DoubleArray(window.elements.size) { iterator.next() }
            }
            return WindowSampleMedium(windowSize, windowStep, samples)
        }

    }

    override fun serializer(): Serializer {
        TODO("Not yet implemented")
    }

    override fun extractElement(at: Int): Any? =
            if (at < samples.size) Window.ofSamples(windowSize, windowStep, samples[at].toList()) else null
}

