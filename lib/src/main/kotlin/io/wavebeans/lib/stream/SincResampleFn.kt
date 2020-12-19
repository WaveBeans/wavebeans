package io.wavebeans.lib.stream

import io.wavebeans.lib.*
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.truncate

/**
 * The function to use a [SincResampleFn] with [Sample] type using [SampleVector] as a container.
 *
 * @param windowSize the size of the window of [SincResampleFn] resampling mechanism. By default 32 samples.
 *
 * @return the [SincResampleFn] initiated with required functions to work with [Sample]s
 */
fun sincResampleFunc(windowSize: Int = 32): SincResampleFn<Sample, SampleVector> {
    return SincResampleFn(
            windowSize = windowSize,
            createVectorFn = { (size, iterator) ->
                sampleVectorOf(size) { _, _ ->
                    if (iterator.hasNext()) iterator.next() else ZeroSample
                }
            },
            extractNextVectorFn = { a ->
                val (size, offset, window, iterator) = a
                sampleVectorOf(size) { i, n ->
                    if (i < n - offset) {
                        window[i + offset]
                    } else {
                        if (iterator.hasNext()) iterator.next()
                        else ZeroSample
                    }
                }

            },
            isNotEmptyFn = { vector -> vector.all { it != ZeroSample } },
            applyFn = { (x, h) -> (h * x).sum() }
    )
}

/**
 * Resampling based on ideas of [Whittaker–Shannon interpolation formula](https://en.wikipedia.org/wiki/Whittaker%E2%80%93Shannon_interpolation_formula).
 * The input sequence is iterated using windows of configured size.
 *
 * On high level the function are called this way:
 *
 *  ```kotlin
 *   val y /** represents the output interface */
 *   fun h() /** represents the filter function, that calculates the needed values of sinc */
 *
 *   // read the initial vector
 *   var x = createVectorFn(windowSize, inputSequenceIterator)
 *   // calculate the first sample and return it
 *   y += applyFn(x, h())
 *
 *   // while the vector do not represent end of signal repeat
 *   while (isNotEmptyFn(x)) {
 *     // calculate new offset
 *     val offset /** the delta is calculated here based on input-output scale factor and current stream position */
 *     // if offset got changed we need to extract next vector
 *     if (offset > 0)
 *        x = extractNextVectorFn(windowSize, offset, x, inputSequenceIterator)
 *      // calculate the sample based on current vector and return it
 *      y += applyFn(x, h())
 *   }
 *  ```
 *
 * The implementation supports any type [T] and [L] by injecting needed type specific functions.
 *
 * * `createVectorFn` - function of two parameters that create a container of type [L] of desired size (1) out of iterator
 *            with elements of type [T] (2). The function called only once when the initial window is being read from
 *            the input sequence.
 * * `extractNextVectorFn` - function of one argument of type [ExtractNextVectorFnArgument] to extract next container of
 *            type [L] out of provided window. The function is called every time the [ExtractNextVectorFnArgument.offset]
 *            is changed.
 * * `isNotEmptyFn` - checks if the container is not empty. The current container is provided via the argument. Returns
 *            `true` if the container is not empty which lead to continue processing the stream, otherwise if `false`
 *            the stream will end.
 * * `applyFn` - function convolve the filter `h` which is a sum of corresponding `sinc` functions values in time
 *            markers of each sample of the window. Expected to return the sum of elements of vector of type [L] as
 *            singular element of type [T], i.e. if `x` is a vector, `h` is a filter, and `*` is convolution operation,
 *            the result expected to be: `(h * x).sum()`
 *
 * @param T the non-nullable type of the singular sample.
 * @param L the type of the container of [T].
 *
 */
class SincResampleFn<T : Any, L : Any>(initParameters: FnInitParameters) : Fn<ResamplingArgument<T>, Sequence<T>>(initParameters) {

    /**
     * Creates an instance of [SincResampleFn] with type-specific functions as instances of [Fn].
     *
     * @param windowSize the size of the windows to use to calculate the [sinc](https://en.wikipedia.org/wiki/Sinc_function) functions filter.
     * @param createVectorFn function of two parameters that create a container of type [L] of desired size (1) out of iterator
     *            with elements of type [T] (2). The function called only once when the initial window is being read from
     *            the input sequence.
     * @param extractNextVectorFn function of one argument of type [ExtractNextVectorFnArgument] to extract next container of
     *            type [L] out of provided window. The function is called every time the [ExtractNextVectorFnArgument.offset]
     *            is changed.
     * @param isNotEmptyFn checks if the container is not empty. The current container is provided via the argument. Returns
     *            `true` if the container is not empty which lead to continue processing the stream, otherwise if `false`
     *            the stream will end.
     * @param applyFn function convolve the filter `h` which is a sum of corresponding `sinc` functions values in time
     *            markers of each sample of the window. Expected to return the sum of elements of vector of type [L] as
     *            singular element of type [T], i.e. if `x` is a vector, `h` is a filter, and `*` is convolution operation,
     *            the result expected to be: `(h * x).sum()`
     */
    constructor(
            windowSize: Int,
            createVectorFn: Fn<Pair<Int, Iterator<T>>, L>,
            extractNextVectorFn: Fn<ExtractNextVectorFnArgument<T, L>, L>,
            isNotEmptyFn: Fn<L, Boolean>,
            applyFn: Fn<Pair<L, DoubleArray>, T>
    ) : this(FnInitParameters()
            .add("windowSize", windowSize)
            .add("createVectorFn", createVectorFn)
            .add("extractNextVectorFn", extractNextVectorFn)
            .add("isNotEmptyFn", isNotEmptyFn)
            .add("applyFn", applyFn)
    )

    /**
     * Creates an instance of [SincResampleFn] with type-specific functions as lambda functions.
     *
     * @param windowSize the size of the windows to use to calculate the [sinc](https://en.wikipedia.org/wiki/Sinc_function) functions filter.
     * @param createVectorFn function of two parameters that create a container of type [L] of desired size (1) out of iterator
     *            with elements of type [T] (2). The function called only once when the initial window is being read from
     *            the input sequence.
     * @param extractNextVectorFn function of one argument of type [ExtractNextVectorFnArgument] to extract next container of
     *            type [L] out of provided window. The function is called every time the [ExtractNextVectorFnArgument.offset]
     *            is changed.
     * @param isNotEmptyFn checks if the container is not empty. The current container is provided via the argument. Returns
     *            `true` if the container is not empty which lead to continue processing the stream, otherwise if `false`
     *            the stream will end.
     * @param applyFn function convolve the filter `h` which is a sum of corresponding `sinc` functions values in time
     *            markers of each sample of the window. Expected to return the sum of elements of vector of type [L] as
     *            singular element of type [T], i.e. if `x` is a vector, `h` is a filter, and `*` is convolution operation,
     *            the result expected to be: `(h * x).sum()`
     */
    constructor(
            windowSize: Int,
            createVectorFn: (Pair<Int, Iterator<T>>) -> L,
            extractNextVectorFn: (ExtractNextVectorFnArgument<T, L>) -> L,
            isNotEmptyFn: (L) -> Boolean,
            applyFn: (Pair<L, DoubleArray>) -> T
    ) : this(
            windowSize,
            wrap(createVectorFn),
            wrap(extractNextVectorFn),
            wrap(isNotEmptyFn),
            wrap(applyFn),
    )

    private val windowSize: Int by lazy {
        initParameters.int("windowSize")
    }
    private val createVectorFn: Fn<Pair<Int, Iterator<T>>, L> by lazy {
        initParameters.fn<Pair<Int, Iterator<T>>, L>("createVectorFn")
    }
    private val extractNextVectorFn: Fn<ExtractNextVectorFnArgument<T, L>, L> by lazy {
        initParameters.fn<ExtractNextVectorFnArgument<T, L>, L>("extractNextVectorFn")
    }
    private val isNotEmptyFn: Fn<L, Boolean> by lazy {
        initParameters.fn<L, Boolean>("isNotEmptyFn")
    }
    private val applyFn: Fn<Pair<L, DoubleArray>, T> by lazy {
        initParameters.fn<Pair<L, DoubleArray>, T>("applyFn")
    }

    override fun apply(argument: ResamplingArgument<T>): Sequence<T> {
        fun sinc(t: Double) = if (t == 0.0) 1.0 else sin(PI * t) / (PI * t)

        require(windowSize > 0) { "Window is too small: windowSize=$windowSize" }
        val fs = argument.inputSampleRate.toDouble()
        val nfs = argument.outputSampleRate.toDouble()

        // windowing
        val streamIterator = argument.inputSequence.iterator()
        var window: L? = null
        var windowStartIndex = 0

        fun extractWindow(on: Double): L {
            if (window == null) {
                window = createVector(windowSize, streamIterator)
            } else {
                val startIndex = windowStartIndex.toDouble()
                val offset = (on - startIndex).toInt()
                if (offset > 0) {
                    window = extractNextVector(windowSize, offset, window!!, streamIterator)
                    windowStartIndex += offset
                }
            }
            return window!!
        }

        // resampling
        fun h(t: Double, x: Double): DoubleArray {
            val p = t * fs - truncate(x * fs)
            return sampleVectorOf(
                    (-windowSize / 2 until windowSize / 2)
                            .map { it - p }
                            .map { sinc(it) }
            )
        }

        var timeMarker = 0.0
        val d = 1.0 / nfs
        return object : Iterator<T> {
            override fun hasNext(): Boolean = isNotEmpty(extractWindow(timeMarker))

            override fun next(): T {
                val sourceTimeMarker = (truncate(timeMarker * fs)) / fs // in seconds
                val x = extractWindow(sourceTimeMarker * fs)
                val h = h(timeMarker, sourceTimeMarker)
                timeMarker += d
                return apply(x, h)
            }
        }.asSequence()
    }

    private fun createVector(size: Int, iterator: Iterator<T>): L = createVectorFn.apply(Pair(size, iterator))

    private fun extractNextVector(size: Int, offset: Int, vector: L, iterator: Iterator<T>): L =
            extractNextVectorFn.apply(ExtractNextVectorFnArgument(size, offset, vector, iterator))

    private fun isNotEmpty(vector: L): Boolean = isNotEmptyFn.apply(vector)

    private fun apply(vector: L, filter: DoubleArray): T = applyFn.apply(Pair(vector, filter))
}

/**
 * The argument of the [SincResampleFn.extractNextVectorFn] function:
 * * [size] - the expected size of the vector to return.
 * * [offset] - the change comparing to previous vector, i.e. the window may move one step at a time or five,
 *              depending on the input output scale rate.
 * * [vector] - the current vector value.
 * * [iterator] - the iterator to read new elements from as needed, may not have enought element, so the function
 *                would need to deal with it on its own.
 */
data class ExtractNextVectorFnArgument<T : Any, L : Any>(
        val size: Int,
        val offset: Int,
        val vector: L,
        val iterator: Iterator<T>
)
