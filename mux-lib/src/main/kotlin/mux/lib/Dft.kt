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

fun fft(x: List<Double>): List<ComplexNumber> {
    val sines = (0 until x.size)
            .map { k -> complexSine(k, x.size).toList() }
            .toTypedArray()

    fun fftImpl(x: List<Double>, n: IntRange): List<ComplexNumber> {
        return if (n.count() == 1) {
            listOf(x[n.first].i)
        } else {
            val xx1 = fftImpl(x.subList(n.first / 2, n.last / 2), n.first..n.last)
            val xx2 = fftImpl(x.subList(n.first / 2, n.last / 2), n.first..n.last)
            val xx = Array(n.last) { 0.i }.toMutableList()
            n.map { k ->
                val t = xx1[k]
                xx[k] = t + sines[k][n.last] * xx2[k]
                xx[k + n.last] = t - sines[k][n.last] * xx2[k]
            }
            xx
        }
    }
    return fftImpl(x, 0..x.size - 1)
}