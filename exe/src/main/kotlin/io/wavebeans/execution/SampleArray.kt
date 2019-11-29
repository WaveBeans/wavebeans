package io.wavebeans.execution

import io.wavebeans.lib.Sample

/**
 * Type alias for [DoubleArray] to keep [Sample]s within primitive [Double] during transfer.
 */
typealias SampleArray = DoubleArray

/**
 * Creates [SampleArray] of the defined [size], initializes the array using provided [initFn] function
 *
 * @param size the size of the [SampleArray] to create. Must be 1 or greater
 * @param initFn the function the initializes the array with values. The parameter of the function is 0-based index of the element i
 */
fun createSampleArray(size: Int, initFn: (Int) -> Sample): SampleArray {
    require(size >= 1) { "Can't create sample array of the size $size" }
    return DoubleArray(size, initFn)
}
