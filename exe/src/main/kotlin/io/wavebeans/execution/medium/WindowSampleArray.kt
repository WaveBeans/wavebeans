package io.wavebeans.execution.medium

import io.wavebeans.lib.Sample
import io.wavebeans.lib.stream.window.Window

/**
 * Type alias for array of [SampleArray] to keep [Sample]s inside [Window] within primitive [Double] during transfer.
 */
typealias WindowSampleArray = Array<SampleArray>

/**
 * Creates [WindowSampleArray] of the defined [size], initializes the array using provided [windowFn] function
 *
 * @param size the size of the [SampleArray] to create. Must be 1 or greater
 * @param windowFn the function the initializes the [Window] of [Sample]s. The parameter of the function is 0-based index of the element i
 */
fun createWindowSampleArray(size: Int, windowFn: (Int) -> Window<Sample>): WindowSampleArray {
    require(size >= 1) { "Can't create sample array of the size $size" }
    return Array(size) {
        val window = windowFn(it)
        val iterator = window.elements.iterator()
        createSampleArray(window.elements.size) { iterator.next() }
    }
}
