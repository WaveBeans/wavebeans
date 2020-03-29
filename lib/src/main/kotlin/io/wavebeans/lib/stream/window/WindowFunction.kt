package io.wavebeans.lib.stream.window

import io.wavebeans.lib.*
import io.wavebeans.lib.stream.map
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos

/**
 * Map function that applies the specified window function on the windowed stream.
 *
 * The function is applied as if `w` is window value array and `f` is window function, then the result is `w * f`,
 * which is multiplication of corresponding elements.
 *
 * **Window function**
 *
 * The window function is intended to generate the window based on the source window size and has the following arguments:
 *  * Input type is `Pair<Int, Int>`, the first is the current index of the value, the second is the overall number of samples in the window.
 *  * Output type is `T`, is the value of the window on the specified index.
 *
 * **Multiply function**
 *
 * The multiply function defines how tow multiply two values coming from the stream and the window:
 *  * The input type is `Pair<T, T>`, which is a pair of sample to multiply, the first is coming from the stream, the second is coming from the window function.
 *  * The output type if `T` which is the result of multiplication.
 *
 * Example of functions:
 *
 * ```kotlin
 * // working with Sample type
 *
 * val windowFunction: Fn<Pair<Int, Int>, Sample> = Fn.wrap { (i, n) ->
 *     // triangular window function
 *     val halfN = n / 2.0
 *     sampleOf(1.0 - abs((i - halfN) / halfN))
 * }
 *
 * val multiplyFn: Fn<Pair<Sample, Sample>, Sample> = Fn.wrap { (a, b) ->
 *     a * b
 * }
 * ```
 *
 * @param T the type of the sample
 */
class MapWindowFn<T : Any>(initParameters: FnInitParameters) : Fn<Window<T>, Window<T>>(initParameters) {

    /**
     * Creates an instance of [MapWindowFn]
     * @param windowFunction function to be used to generate the values. Has two values:
     *  1. the current index to generate for and
     *  2. overall amount of sample-entities will be asked to generate.
     * @param multiplyFn -- function to be used to multiply the corresponding sample-entities while applying the window function.
     */
    constructor(windowFunction: Fn<Pair<Int, Int>, T>, multiplyFn: Fn<Pair<T, T>, T>) : this(FnInitParameters()
            .add("fn", windowFunction)
            .add("multiplyFn", multiplyFn)
    )

    override fun apply(argument: Window<T>): Window<T> {
        val fn = initParams.fn<Pair<Int, Int>, T>("fn")
        val multiplyFn = initParams.fn<Pair<T, T>, T>("multiplyFn")

        val windowSize = argument.elements.size

        val windowFunction = (0 until windowSize).asSequence()
                .map { index -> fn.apply(Pair(index, windowSize)) }
        return argument.copy(
                elements = argument.elements.asSequence()
                        .zip(windowFunction)
                        .map { multiplyFn.apply(it) }
                        .toList()
        )
    }
}

fun <T : Any> BeanStream<Window<T>>.windowFunction(
        func: Fn<Pair<Int, Int>, T>,
        multiplyFn: Fn<Pair<T, T>, T>
): BeanStream<Window<T>> {
    return this.map(MapWindowFn(func, multiplyFn))
}

fun <T : Any> BeanStream<Window<T>>.windowFunction(
        func: (Pair<Int, Int>) -> T,
        multiplyFn: (Pair<T, T>) -> T
): BeanStream<Window<T>> {
    return this.windowFunction(Fn.wrap(func), Fn.wrap(multiplyFn))
}


/**
 * Applies [MapWindowFn] with specified function as a window function over the stream of windowed samples.
 *
 * @param func the function to multiply window by.
 */
fun BeanStream<Window<Sample>>.windowFunction(func: Fn<Pair<Int, Int>, Sample>): BeanStream<Window<Sample>> {
    return this.windowFunction(func, Fn.wrap { it.first * it.second })
}

/**
 * Applies [MapWindowFn] with specified function as a window function over the stream of windowed samples.
 *
 * @param func the function to multiply window by.
 */
fun BeanStream<Window<Sample>>.windowFunction(func: (Pair<Int, Int>) -> Sample): BeanStream<Window<Sample>> {
    return this.windowFunction(Fn.wrap(func))
}

/**
 * Applies [MapWindowFn] with [rectangle](https://en.wikipedia.org/wiki/Window_function#Rectangular_window)
 * as a window function over the stream of windowed samples.
 */
fun BeanStream<Window<Sample>>.rectangle(): BeanStream<Window<Sample>> {
    return this.windowFunction { sampleOf(1.0) }
}

/**
 * Applies [MapWindowFn] with [triangular](https://en.wikipedia.org/wiki/Window_function#Triangular_window)
 * as a window function over the stream of windowed samples.
 */
fun BeanStream<Window<Sample>>.triangular(): BeanStream<Window<Sample>> {
    return this.windowFunction { (i, n) ->
        val halfN = n / 2.0
        sampleOf(1.0 - abs((i - halfN) / halfN))
    }
}

/**
 * Applies [MapWindowFn] with [blackman](https://en.wikipedia.org/wiki/Window_function#Blackman_window)
 * as a window function over the stream of windowed samples.
 */
fun BeanStream<Window<Sample>>.blackman(): BeanStream<Window<Sample>> {
    return this.windowFunction { (i, n) ->
        val a0 = 0.42
        val a1 = 0.5
        val a2 = 0.08
        sampleOf(a0 - a1 * cos(2 * PI * i / n) + a2 * cos(4 * PI * i / n))
    }
}

/**
 * Applies [MapWindowFn] with [hamming](https://en.wikipedia.org/wiki/Window_function#Hann_and_Hamming_windows)
 * as a window function over the stream of windowed samples.
 */
fun BeanStream<Window<Sample>>.hamming(): BeanStream<Window<Sample>> {
    return this.windowFunction { (i, n) ->
        val a0 = 25.0 / 46.0
        sampleOf(a0 - (1 - a0) * cos(2 * PI * i / n))
    }
}