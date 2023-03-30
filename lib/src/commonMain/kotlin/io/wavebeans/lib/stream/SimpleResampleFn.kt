package io.wavebeans.lib.stream

import io.wavebeans.lib.Fn
import io.wavebeans.lib.FnInitParameters
import io.wavebeans.lib.wrap
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
 */
class SimpleResampleFn<T : Any>(initParameters: FnInitParameters) : Fn<ResamplingArgument<T>, Sequence<T>>(initParameters) {

    /**
     * Creates an instance of [SimpleResampleFn].
     *
     * @param reduceFn reduce function as an instance if [Fn] is called only during downsamping and should convert the List<[T]> to the singular value [T].
     */
    constructor(reduceFn: Fn<List<T>, T>) : this(FnInitParameters().add("reduceFn", reduceFn))

    /**
     * Creates an instance of [SimpleResampleFn].
     *
     * @param reduceFn reduce function is called only during downsamping and should convert the List<[T]> to the singular value [T].
     */
    constructor(reduceFn: (List<T>) -> T) : this(wrap(reduceFn))

    /**
     * Creates an instance of [SimpleResampleFn] without reduce function.
     */
    constructor() : this(wrap {
        throw IllegalStateException("Using ${SimpleResampleFn::class} as a " +
                "resample function, but reduce function is not defined")
    })

    private val reduceFn: Fn<List<T>, T> by lazy { initParameters.fn<List<T>, T>("reduceFn") }

    override fun apply(argument: ResamplingArgument<T>): Sequence<T> {
        val reverseFactor = 1.0f / argument.resamplingFactor

        return if (argument.resamplingFactor == truncate(argument.resamplingFactor) || reverseFactor == truncate(reverseFactor)) {
            when {
                argument.resamplingFactor > 1 -> argument.inputSequence
                        .map { sample -> (0 until argument.resamplingFactor.toInt()).asSequence().map { sample } }
                        .flatten()
                argument.resamplingFactor < 1 -> argument.inputSequence
                        .windowed(reverseFactor.toInt(), reverseFactor.toInt(), partialWindows = true)
                        .map { samples -> reduceFn.apply(samples) }
                else -> argument.inputSequence
            }
        } else {
            throw UnsupportedOperationException("That implementation doesn't support non-integer " +
                    "input-output scale factor, but it is ${argument.resamplingFactor} or reversed $reverseFactor")
        }
    }
}