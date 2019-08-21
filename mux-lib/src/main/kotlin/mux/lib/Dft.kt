package mux.lib

import mux.lib.math.*
import java.lang.IllegalArgumentException
import kotlin.math.*

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

/**
 * Reference FFT implementation.
 * Based on https://en.wikipedia.org/wiki/Cooley%E2%80%93Tukey_FFT_algorithm#Data_reordering,_bit_reversal,_and_in-place_algorithms
 */
fun fft(x: Sequence<ComplexNumber>, n: Int, inversed: Boolean = false): Sequence<ComplexNumber> {

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
    val c = (if (inversed) -2.0 else 2.0) * PI
    while (m <= n) {
        val y = c / m
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

    return if (inversed)
        xx.asSequence().map { it / n }
    else
        xx.asSequence()
}

fun Sequence<ComplexNumber>.zeropad(m: Int, n: Int): Sequence<ComplexNumber> {
    val m1 = ceil(m / 2.0).toInt()
    val m2 = m - m1
    val r = n - m
    return this.windowed(m, m, partialWindows = true) { l ->
        val firstHalfToEnd = l.take(m2)
        val rest = (0 until (m - l.size)).asSequence().map { CZERO }
        l.asSequence().drop(m2)
                .take(m1) // second half to the beginning
                .plus((0 until r).asSequence().map { CZERO }) // zero padding
                .plus(rest) // if not enough elements read (<m) the add some here
                .plus(firstHalfToEnd)
    }.flatMap { it.asSequence() }
}


fun Sequence<ComplexNumber>.hanningWindow(n: Int) = this
        .zip(HanningWindow(n).asSequence())
        .map { it.first * it.second }

abstract class Window(val n: Int) {

    fun asSequence(): Sequence<Double> = (0 until n).asSequence().map { func(it) }

    protected abstract fun func(i: Int): Double
}

class RectangleWindow(n: Int) : Window(n) {
    override fun func(i: Int): Double = 1.0
}

class TriangularWindow(n: Int) : Window(n) {

    private val halfN = n / 2

    override fun func(i: Int): Double = 1.0 - abs((i - halfN) / halfN)
}

class SineWindow(n: Int) : Window(n) {
    override fun func(i: Int): Double = sin(PI * i / n)
}

class HanningWindow(n: Int) : Window(n) {
    override fun func(i: Int): Double {
        val sinX = sin(PI * i / n)
        return sinX * sinX
    }

}