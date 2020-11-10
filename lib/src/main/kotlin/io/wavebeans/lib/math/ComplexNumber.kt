package io.wavebeans.lib.math

import kotlinx.serialization.Serializable
import kotlin.math.*

val CZERO = 0.r

@Serializable
data class ComplexNumber(val re: Double, val im: Double) : Comparable<ComplexNumber> {

    fun abs(): Double = sqrt(re * re + im * im)

    fun phi(): Double = atan2(im, re)

    override fun toString(): String {
        val r = this.re.toString()
        val i = if (this.im < 0) "-${abs(this.im)}" else "+${abs(this.im)}"
        return "$r${i}i"
    }

    override fun compareTo(other: ComplexNumber): Int {
        return when {
            this.re - other.re > 1e-15 -> 1
            this.re - other.re < -1e-15 -> -1
            abs(this.re - other.re) < 1e-15 && this.im - other.im > 1e-15 -> 1
            abs(this.re - other.re) < 1e-15 && this.im - other.im < -1e-15 -> -1
            else -> 0
        }

    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ComplexNumber

        return this.compareTo(other) == 0
    }

    override fun hashCode(): Int {
        var result = re.hashCode()
        result = 31 * result + im.hashCode()
        return result
    }

}

@Suppress("NOTHING_TO_INLINE")
inline fun complex(re: Double, im: Double): ComplexNumber = ComplexNumber(re, im)
@Suppress("NOTHING_TO_INLINE")
inline fun complexOfPolarForm(abs: Double, phi: Double): ComplexNumber = complex(abs * cos(phi), abs * sin(phi))

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

