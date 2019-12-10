package io.wavebeans.execution.medium

import io.wavebeans.lib.stream.fft.FftSample

/**
 * Type alias for array of [FftSample]
 */
typealias FftSampleArray = Array<FftSample>

/**
 * Creates [FftSampleArray] of the defined [size], initializes the array using provided [initFn] function
 *
 * @param size the size of the [FftSampleArray] to create. Must be 1 or greater
 * @param initFn the function the initializes the array with values. The parameter of the function is 0-based index of the element i
 */
fun createFftSampleArray(size: Int, initFn: (Int) -> FftSample): FftSampleArray {
    require(size >= 1) { "Can't create sample array of the size $size" }
    return Array(size, initFn)
}
