package mux.lib.stream

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

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
