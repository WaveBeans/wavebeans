package mux.lib

import mux.lib.math.*
import java.lang.IllegalStateException
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

    // Danielson-Lanzcos routine
    var mmax = 2
    while (n > mmax) {
        val istep = mmax shl 1
        val theta = -(2 * PI / mmax)
        val wtemp = sin(theta / 2).r
        val wp = -2 * wtemp * wtemp + sin(theta).i
        var w = 1.r
        for (m in 1..mmax step 2) {
            for (i in m..n step istep) {
                val j = i + mmax
                val temp = w * xx[j / 2]
                xx[j / 2] = xx[i / 2] - temp
                xx[i / 2] += temp
            }
            w += w * wp
        }
        mmax = istep
    }

    return xx.asSequence()
}
