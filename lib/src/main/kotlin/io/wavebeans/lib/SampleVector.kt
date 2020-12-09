package io.wavebeans.lib

import io.wavebeans.lib.stream.window.Window

/**
 * An array of [Sample]s which allows to apply certain optimizations.
 */
typealias SampleVector = DoubleArray

/**
 * Empty-sized [SampleVector].
 */
val EmptySampleVector: SampleVector = SampleVector(0)

/**
 * Creates a [SampleVector] of the list of elements. The elements are copied into underlying array as a reference.
 *
 * @param list the list of sample to create from.
 *
 * @return the new [SampleVector].
 */
@Suppress("NOTHING_TO_INLINE")
fun sampleVectorOf(list: List<Sample>): SampleVector {
    val i = list.iterator()
    return SampleVector(list.size) { i.next() }
}

/**
 * Creates a [SampleVector].
 *
 * @param sample the samples to create an vecotr from.
 *
 * @return the new [SampleVector].
 */
@Suppress("NOTHING_TO_INLINE")
fun sampleVectorOf(vararg sample: Sample): SampleVector = SampleVector(sample.size) { sample[it] }

/**
 * Creates a [SampleVector] of the [Window] of [Sample]s. The elements are copied into underlying array as a reference.
 *
 * @param window the windowed samples to create from.
 *
 * @return the new [SampleVector].
 */
@Suppress("NOTHING_TO_INLINE")
fun sampleVectorOf(window: Window<Sample>): SampleVector = sampleVectorOf(window.elements)

/**
 * Creates a [SampleVector] using generator function
 *
 * @param n the size of the vector
 * @param generator generator function that takes a pair of integers (Index and Total=n) as an argument and returns the [Sample].
 *
 *  @return the new [SampleVector].
 */
fun sampleVectorOf(n: Int, generator: (Int, Int) -> Sample): SampleVector = SampleVector(n) { generator(it, n) }

/**
 * Applies the operation on two vectors, operation is consequently called on each corresponding pair.
 * The vectors might be different length, the result vector has the maximum length of both provided.
 * The absent elements are substituted with [ZeroSample].
 *
 * Returns `null` only if both operands are `null`, otherwise at least zero-length vector is returned.
 *
 * @receiver the source for first operand of the operation.
 * @param other the source for second operand of the operation.
 * @param operation the function to apply on corresponding [Sample]s
 *
 * @return the new [SampleVector] that has applied the operation on corresponding elements,
 *          it has zero-length in case of both vector are zero-length, or null if both vectors are null.
 */
inline fun SampleVector?.apply(other: SampleVector?, operation: (Sample, Sample) -> Sample): SampleVector? {
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

/**
 * Applies via [apply] the sum of [Sample]s operation.
 */
operator fun SampleVector?.plus(other: SampleVector?): SampleVector? = apply(other) { a, b -> a + b }

/**
 * Applies via [apply] the sum of [Sample]s operation.
 */
@JvmName("plusNonNullable")
operator fun SampleVector.plus(other: SampleVector): SampleVector = (this as SampleVector? + other as SampleVector?)!!

/**
 * Applies via [apply] the subtract of [Sample]s operation.
 */
operator fun SampleVector?.minus(other: SampleVector?): SampleVector? = apply(other) { a, b -> a - b }

/**
 * Applies via [apply] the subtract of [Sample]s operation.
 */
@JvmName("minusNonNullable")
operator fun SampleVector.minus(other: SampleVector): SampleVector = (this as SampleVector? - other as SampleVector?)!!

/**
 * Applies via [apply] the multiply of [Sample]s operation.
 */
operator fun SampleVector?.times(other: SampleVector?): SampleVector? = apply(other) { a, b -> a * b }

/**
 * Applies via [apply] the multiply of [Sample]s operation.
 */
@JvmName("timesNonNullable")
operator fun SampleVector.times(other: SampleVector): SampleVector = (this as SampleVector? * other as SampleVector?)!!

/**
 * Applies via [apply] the division of [Sample]s operation.
 */
operator fun SampleVector?.div(other: SampleVector?): SampleVector? = apply(other) { a, b -> a / b }

/**
 * Applies via [apply] the division of [Sample]s operation.
 */
@JvmName("divNonNullable")
operator fun SampleVector.div(other: SampleVector): SampleVector = (this as SampleVector? / other as SampleVector?)!!

/**
 * Creates a [Window] of [Sample]s out of the window. The [Window.size] is populated as per the [SampleVector] size,
 * the [Window.step] is optionally provided by parameter, by default it assumed to have the same value as size.
 *
 * @param step if not null then used as a [Window.step]
 *
 * @return newly created [Window] of [Sample]s, where all elements are copied over by reference.
 */
fun SampleVector.window(step: Int? = null): Window<Sample> =
        Window.ofSamples(this.size, step ?: this.size, this.toList())

/**
 * Adds a scalar [this] to each element of the sample vector [vector].
 *
 * @return new instance of the [SampleVector] with operation performed.
 */
operator fun Sample.plus(vector: SampleVector): SampleVector = sampleVectorOf(vector.size) { i, _ -> vector[i] + this}
/**
 * Adds a scalar [scalar] to each element of the sample vector [this].
 */
operator fun SampleVector.plus(scalar: Sample): SampleVector = sampleVectorOf(this.size) { i, _ -> this[i] + scalar}
/**
 * Subtracts a scalar [this] from each element of the sample vector [vector].
 */
operator fun Sample.minus(vector: SampleVector): SampleVector = sampleVectorOf(vector.size) { i, _ -> this - vector[i]}
/**
 * Subtracts a scalar [scalar] from each element of the sample vector [this].
 */
operator fun SampleVector.minus(scalar: Sample): SampleVector = sampleVectorOf(this.size) { i, _ -> this[i] - scalar}
/**
 * Multiplies a scalar [this] to each element of the sample vector [vector].
 */
operator fun Sample.times(vector: SampleVector): SampleVector = sampleVectorOf(vector.size) { i, _ -> vector[i] * this}
/**
 * Multiplies a scalar [scalar] to each element of the sample vector [this].
 */
operator fun SampleVector.times(scalar: Sample): SampleVector = sampleVectorOf(this.size) { i, _ -> this[i] * scalar}
/**
 * Divides each element of sample vector [vector] by a scalar [this].
 */
operator fun Sample.div(vector: SampleVector): SampleVector = sampleVectorOf(vector.size) { i, _ -> this / vector[i]}
/**
 * Divides a scalar [scalar] by each element of the sample vector [this].
 */
operator fun SampleVector.div(scalar: Sample): SampleVector = sampleVectorOf(this.size) { i, _ -> this[i] / scalar}

