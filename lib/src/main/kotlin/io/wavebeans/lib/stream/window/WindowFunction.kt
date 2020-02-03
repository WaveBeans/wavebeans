package io.wavebeans.lib.stream.window

import io.wavebeans.lib.*
import io.wavebeans.lib.stream.map
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos

class MapWindowFn<T : Any>(initParameters: FnInitParameters) : Fn<Window<T>, Window<T>>(initParameters) {

    constructor(windowFunction: Fn<Pair<Int, Int>, T>, multiplyFn: Fn<Pair<T, T>, T>) : this(FnInitParameters()
            .addObj("fn", windowFunction) { it.asString() }
            .addObj("multiplyFn", multiplyFn) { it.asString() }
    )

    override fun apply(argument: Window<T>): Window<T> {
        val fn = initParams.obj("fn") { fromString<Pair<Int, Int>, T>(it) }
        val multiplyFn = initParams.obj("multiplyFn") { fromString<Pair<T, T>, T>(it) }

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

fun BeanStream<Window<Sample>>.windowFunction(func: Fn<Pair<Int, Int>, Sample>): BeanStream<Window<Sample>> {
    return this.map(MapWindowFn(func, Fn.wrap { it.first * it.second }))
}

fun BeanStream<Window<Sample>>.windowFunction(func: (Pair<Int, Int>) -> Sample): BeanStream<Window<Sample>> {
    return this.windowFunction(Fn.wrap(func))
}

fun BeanStream<Window<Sample>>.rectangle(): BeanStream<Window<Sample>> {
    return this.windowFunction { sampleOf(1.0) }
}

fun BeanStream<Window<Sample>>.triangular(): BeanStream<Window<Sample>> {
    return this.windowFunction { (i, n) ->
        val halfN = n / 2.0
        sampleOf(1.0 - abs((i - halfN) / halfN))
    }
}

fun BeanStream<Window<Sample>>.blackman(): BeanStream<Window<Sample>> {
    return this.windowFunction { (i, n) ->
        val a0 = 0.42
        val a1 = 0.5
        val a2 = 0.08
        sampleOf(a0 - a1 * cos(2 * PI * i / n) + a2 * cos(4 * PI * i / n))
    }
}

fun BeanStream<Window<Sample>>.hamming(): BeanStream<Window<Sample>> {
    return this.windowFunction { (i, n) ->
        val a0 = 25.0 / 46.0
        sampleOf(a0 - (1 - a0) * cos(2 * PI * i / n))
    }
}