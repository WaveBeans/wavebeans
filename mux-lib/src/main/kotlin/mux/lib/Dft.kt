package mux.lib

import mux.lib.math.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun complexSine(k: Int, n: Int): Sequence<ComplexNumber> {
    val c = 2.0 * PI * k / n
    return (0 until n).asSequence()
            .map { x ->
                val xc = x * c
                cos(xc) - sin(xc).i
            }
}

fun reversedComplexSine(k: Int, n: Int): Sequence<ComplexNumber> {
    val c = 2.0 * PI * k / n
    return (0 until n).asSequence()
            .map { x ->
                val xc = x * c
                cos(xc) + sin(xc).i
            }
}

fun dft(x: List<Double>): Sequence<ComplexNumber> {
    val n = x.count()
    val sines = (0 until n)
            .map { k -> complexSine(k, n) }
            .toTypedArray()

    return (0 until n).asSequence()
            .map { k ->
                x.asSequence()
                        .zip(sines[k])
                        .fold(0.r) { a, p -> a + p.first * p.second }
            }
}

fun idft(x: List<Double>): Sequence<ComplexNumber> {
    val n = x.count()
    val sines = (0 until n)
            .map { k -> reversedComplexSine(k, n).toList() }
            .toTypedArray()


    return (0 until n).asSequence()
            .map { i ->
                (0 until n)
                        .map { k -> sines[k][i] * x[k] }
                        .fold(0.r) { a, v -> a + v } / n
            }
}