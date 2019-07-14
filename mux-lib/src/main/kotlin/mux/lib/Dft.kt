package mux.lib

import mux.lib.math.*
import java.lang.IllegalArgumentException
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

fun dft(x: Sequence<ComplexNumber>, n: Int): Sequence<ComplexNumber> {
    val sines = (0 until n)
            .map { k -> complexSine(k, n) }
            .toTypedArray()

    return (0 until n).asSequence()
            .map { k ->
                x.asSequence()
                        .take(n)
                        .zip(sines[k])
                        .fold(0.r) { a, p -> a + p.first * p.second }
            }
}

fun idft(x: Sequence<ComplexNumber>, n: Int): Sequence<ComplexNumber> {
    val sines = (0 until n)
            .map { k -> reversedComplexSine(k, n).toList() }
            .toTypedArray()


    return (0 until n).asSequence()
            .map { i ->
                x.take(n)
                        .mapIndexed { k, x -> sines[k][i] * x }
                        .fold(0.r) { a, v -> a + v } / n
            }
}

fun fft(x: Sequence<ComplexNumber>, n: Int): Sequence<ComplexNumber> {

    if (n == 0 || n and (n - 1) != 0) throw IllegalArgumentException("N should be power of 2 but $n found")

    val ii = x.iterator()
    val xx = Array(n) {
        if (ii.hasNext())
            ii.next()
        else
            throw IllegalStateException("input sequence doesn't have enough elements, expected to be $n ")
    }

    fun swap(a: Int, b: Int) {
        val t = xx[a]
        xx[a] = xx[b]
        xx[b] = t
    }

    // reverse-binary indexing
    var j = 0
    for (i in 1 until n) {
        var m = n / 2
        while (m in 1..j) {
            j -= m
            m = m ushr 1
        }
        j += m
        if (j > i) {
            swap(j, i)
        }
    }

    // iterative fft
    var m = 2
    while (m <= n) {
        val y = -2.0 * PI / m
        val wm = cos(y) + sin(y).i
        for (k in 0 until n step m) {
            var w = 1.r
            for (l in 0 until m / 2) {
                val t = w * xx[k + l + m / 2]
                val u = xx[k + l]
                xx[k + l] = u + t
                xx[k + l + m / 2] = u - t
                w *= wm
            }
        }
        m = m shl 1
    }

    return xx.asSequence()
}
