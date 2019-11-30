package io.wavebeans.lib

import io.wavebeans.lib.stream.ScalarOp
import io.wavebeans.lib.stream.StreamOp
import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor
import kotlin.math.round


const val INT_24BIT_MAX_VALUE = 8388608

/** Internal representation of sample. */
typealias Sample = Double

const val ZeroSample: Sample = 0.0

@Suppress("NOTHING_TO_INLINE")
inline fun sampleOf(x: Long): Sample = sampleOf(x.toDouble() / Long.MAX_VALUE)

@Suppress("NOTHING_TO_INLINE")
inline fun sampleOf(x: Int, as24bit: Boolean = false): Sample =
        if (as24bit)
            sampleOf(x.toDouble() / INT_24BIT_MAX_VALUE)
        else
            sampleOf(x.toDouble() / Int.MAX_VALUE)

@Suppress("NOTHING_TO_INLINE")
inline fun sampleOf(x: Short): Sample = sampleOf(x.toDouble() / Short.MAX_VALUE)

@Suppress("NOTHING_TO_INLINE")
inline fun sampleOf(x: Byte): Sample = sampleOf(x.toDouble() / Byte.MAX_VALUE)

@Suppress("NOTHING_TO_INLINE")
inline fun sampleOf(x: Double): Sample = /*if (x > 1.0 || x < -1.0) throw UnsupportedOperationException("x should belong to range [-1.0, 1.0] but it's $x") else */x

@Suppress("NOTHING_TO_INLINE")
inline fun Sample.asLong(): Long = round(this * Long.MAX_VALUE).toLong()

@Suppress("NOTHING_TO_INLINE")
inline fun Sample.asInt(): Int = round(this * Int.MAX_VALUE).toInt()

@Suppress("NOTHING_TO_INLINE")
inline fun Sample.as24BitInt(): Int {
    return round(this * INT_24BIT_MAX_VALUE).toInt()
}

@Suppress("NOTHING_TO_INLINE")
inline fun Sample.asShort(): Short = round(this * Short.MAX_VALUE).toShort()

@Suppress("NOTHING_TO_INLINE")
inline fun Sample.asByte(): Byte = round(this * Byte.MAX_VALUE).toByte()

@Suppress("NOTHING_TO_INLINE")
inline fun Byte.asUnsignedByte(): Int {
    val it = this.toInt() and 0xFF
    return it and 0x7F or (it.inv() and 0x80)
}

interface SampleStreamOp : StreamOp<Sample>

interface SampleScalarOp : ScalarOp<Double, Sample>

object SamplePlusStreamOp : SampleStreamOp {
    override fun apply(a: Sample, b: Sample): Sample = a + b
}

object SampleMinusStreamOp : SampleStreamOp {
    override fun apply(a: Sample, b: Sample): Sample = a - b
}

object SampleTimesStreamOp : SampleStreamOp {
    override fun apply(a: Sample, b: Sample): Sample = a * b
}

object SampleDivStreamOp : SampleStreamOp {
    override fun apply(a: Sample, b: Sample): Sample = a / b
}

object SamplePlusScalarOp : SampleScalarOp {
    override fun apply(a: Sample, b: Double): Sample = a + b
}

object SampleMinusScalarOp : SampleScalarOp {
    override fun apply(a: Sample, b: Double): Sample = a - b
}

object SampleTimesScalarOp : SampleScalarOp {
    override fun apply(a: Sample, b: Double): Sample = a * b
}

object SampleDivScalarOp : SampleScalarOp {
    override fun apply(a: Sample, b: Double): Sample = a / b
}

@Serializer(forClass = SampleStreamOp::class)
object StreamOpSerializer {

    override val descriptor: SerialDescriptor
        get() = StringDescriptor.withName("StreamOp")

    override fun deserialize(decoder: Decoder): SampleStreamOp =
            when (val operation = decoder.decodeString()) {
                "+" -> SamplePlusStreamOp
                "-" -> SampleMinusStreamOp
                "*" -> SampleTimesStreamOp
                "/" -> SampleDivStreamOp
                else -> throw UnsupportedOperationException("`$operation` is not supported")
            }

    override fun serialize(encoder: Encoder, obj: SampleStreamOp) =
            when (obj) {
                is SampleMinusStreamOp -> encoder.encodeString("-")
                is SamplePlusStreamOp -> encoder.encodeString("+")
                is SampleTimesStreamOp -> encoder.encodeString("*")
                is SampleDivStreamOp -> encoder.encodeString("/")
                else -> throw UnsupportedOperationException("${obj::class} is not supported")
            }

}

@Serializer(forClass = SampleStreamOp::class)
object ScalarOpSerializer {

    override val descriptor: SerialDescriptor
        get() = StringDescriptor.withName("StreamOp")

    override fun deserialize(decoder: Decoder): SampleStreamOp =
            when (val operation = decoder.decodeString()) {
                "+" -> SamplePlusStreamOp
                "-" -> SampleMinusStreamOp
                "*" -> SampleTimesStreamOp
                "/" -> SampleDivStreamOp
                else -> throw UnsupportedOperationException("`$operation` is not supported")
            }

    override fun serialize(encoder: Encoder, obj: SampleStreamOp) =
            when (obj) {
                is SampleMinusStreamOp -> encoder.encodeString("-")
                is SamplePlusStreamOp -> encoder.encodeString("+")
                is SampleTimesStreamOp -> encoder.encodeString("*")
                is SampleDivStreamOp -> encoder.encodeString("/")
                else -> throw UnsupportedOperationException("${obj::class} is not supported")
            }

}

