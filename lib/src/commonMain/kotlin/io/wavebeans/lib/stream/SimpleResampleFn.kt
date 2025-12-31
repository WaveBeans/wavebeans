package io.wavebeans.lib.stream

import kotlin.math.truncate

/**
 * Simple resample function that upsamples via duplicating samples, and downsamples by windowing and then reducing
 * down with `reduceFn` down to one sample of type [T]. This method supports only integer
 * [ResamplingArgument.resamplingFactor] (direct or reversed -- upsampling or downsampling accordingly).
 *
 * Reduce function `reduceFn` is called only during downsamping and should convert the List<[T]> to the singular value [T].
 *
 * Downsampling calls reduce function on each group of `1.0 / [ResamplingArgument.resamplingFactor]` elements:
 *
 * ```text
 * [ 1 1 2 2 3 3 4 4 5 5 ] --(x0.5)--> reduceFn(::average) --> [ 1 2 3 4 5 ]
 * ```
 *
 * Upsampling duplicates elements for [ResamplingArgument.resamplingFactor] times:
 *
 * ```text
 * [ 1 2 3 4 5 ] --(x2)--> [ 1 1 2 2 3 3 4 4 5 5 ]
 * ```
 *
 * @param [T] the of the element being resampled.
 * @param reduceFn reduce function is called only during downsamping and should convert the List<[T]> to the singular value [T].
 */
class SimpleResampleFn<T : Any>(
        private val reduceFn: (List<T>) -> T
) {

    operator fun invoke(argument: ResamplingArgument<T>): Sequence<T> {
        val reverseFactor = 1.0f / argument.resamplingFactor

        return if (argument.resamplingFactor == truncate(argument.resamplingFactor) || reverseFactor == truncate(reverseFactor)) {
            when {
                argument.resamplingFactor > 1 -> argument.inputSequence
                        .map { sample -> (0 until argument.resamplingFactor.toInt()).asSequence().map { sample } }
                        .flatten()
                argument.resamplingFactor < 1 -> argument.inputSequence
                        .windowed(reverseFactor.toInt(), reverseFactor.toInt(), partialWindows = true)
                        .map { samples -> reduceFn(samples) }
                else -> argument.inputSequence
            }
        } else {
            throw UnsupportedOperationException("That implementation doesn't support non-integer " +
                    "input-output scale factor, but it is ${argument.resamplingFactor} or reversed $reverseFactor")
        }
    }
}