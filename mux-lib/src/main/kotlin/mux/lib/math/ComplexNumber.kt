package mux.lib.math

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

typealias ComplexNumber = Array<Double>

@Suppress("NOTHING_TO_INLINE")
inline fun complex(re: Double, im: Double): ComplexNumber = arrayOf(re, im)

val ComplexNumber.re: Double
    inline get() = this[0]

val ComplexNumber.im: Double
    inline get() = this[1]

val Number.r: ComplexNumber
    get() = complex(this.toDouble(), 0.0)

val Number.i: ComplexNumber
    get() = complex(0.0, this.toDouble())


operator fun Number.plus(a: ComplexNumber): ComplexNumber = complex(this.toDouble() + a.re, a.im)

operator fun Number.minus(a: ComplexNumber): ComplexNumber = complex(this.toDouble() - a.re, -a.im)

operator fun Number.times(a: ComplexNumber): ComplexNumber = complex(a.re * this.toDouble(), a.im * this.toDouble())

operator fun ComplexNumber.plus(a: Number): ComplexNumber = complex(this.re + a.toDouble(), this.im)

operator fun ComplexNumber.minus(a: Number): ComplexNumber = complex(this.re - a.toDouble(), this.im)

operator fun ComplexNumber.times(a: Number): ComplexNumber = complex(this.re * a.toDouble(), this.im * a.toDouble())

operator fun ComplexNumber.div(a: Number): ComplexNumber = complex(this.re / a.toDouble(), this.im / a.toDouble())

operator fun ComplexNumber.plus(a: ComplexNumber): ComplexNumber = complex(this.re + a.re, this.im + a.im)

operator fun ComplexNumber.minus(a: ComplexNumber): ComplexNumber = complex(this.re - a.re, this.im - a.im)

operator fun ComplexNumber.unaryMinus(): ComplexNumber = complex(-this.re, -this.im)

operator fun ComplexNumber.unaryPlus(): ComplexNumber = this

operator fun ComplexNumber.times(a: ComplexNumber): ComplexNumber =
        complex(
                re * a.re - im * a.im,
                re * a.im + im * a.re
        )

fun ComplexNumber.abs(): Double = sqrt(re * re + im * im)

fun ComplexNumber.phi(): Double = atan2(im, re)

fun ComplexNumber?.string(): String {
    if (this == null) return "undefined"
    val r = this.re.toString()
    val i = if (this.im < 0) "+${abs(this.im)}" else "-${this.im}"
    return "$r${i}j"
}

