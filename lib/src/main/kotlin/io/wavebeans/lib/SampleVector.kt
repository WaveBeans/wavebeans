package io.wavebeans.lib

import io.wavebeans.lib.stream.window.Window

typealias SampleVector = DoubleArray

val EmptySampleVector: SampleVector = SampleVector(0)

@Suppress("NOTHING_TO_INLINE")
fun sampleVectorOf(list: List<Sample>): SampleVector {
    val i = list.iterator()
    return SampleVector(list.size) { i.next() }
}

@Suppress("NOTHING_TO_INLINE")
fun sampleVectorOf(vararg sample: Sample): SampleVector = SampleVector(sample.size) { sample[it] }

@Suppress("NOTHING_TO_INLINE")
fun sampleVectorOf(window: Window<Sample>): SampleVector = sampleVectorOf(window.elements)

fun SampleVector?.apply(other: SampleVector?, operation: (Sample, Sample) -> Sample): SampleVector? {
    return when {
        this == null && other == null -> null
        else -> {
            val resultSize = kotlin.math.max(this?.size ?: 0, other?.size ?: 0)
            val result = SampleVector(resultSize)
            for (i in 0 until resultSize) {
                result[i] = operation(
                        this?.elementAtOrNull(i) ?: ZeroSample,
                        other?.elementAtOrNull(i) ?: ZeroSample
                )
            }
            result
        }
    }
}

operator fun SampleVector?.plus(other: SampleVector?): SampleVector? = apply(other) { a, b -> a + b }
operator fun SampleVector?.minus(other: SampleVector?): SampleVector? = apply(other) { a, b -> a - b }
operator fun SampleVector?.times(other: SampleVector?): SampleVector? = apply(other) { a, b -> a * b }
operator fun SampleVector?.div(other: SampleVector?): SampleVector? = apply(other) { a, b -> a / b }