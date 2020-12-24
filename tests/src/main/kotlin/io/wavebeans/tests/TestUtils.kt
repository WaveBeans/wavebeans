package io.wavebeans.tests

import assertk.Assert
import assertk.all
import assertk.assertions.*
import assertk.assertions.support.show
import io.wavebeans.lib.SampleVector

fun <T> Assert<Iterable<T>>.eachIndexed(expectedSize: Int? = null, f: (Assert<T>, Int) -> Unit) = given { actual ->
    all {
        expectedSize?.let { assertThat(actual.count(), "elements count").isEqualTo(it) }
        actual.forEachIndexed { index, item ->
            f(assertThat(item, name = "${name ?: ""}${show(index, "[]")}"), index)
        }
    }
}

fun <T> Assert<List<T>>.isContainedBy(
        expected: List<T>,
        isEqual: (T, T) -> Boolean = { a, b -> a == b }
) = isContainedBy(expected, isEqual, null)

/**
 * [isContainedBy] with debug information output.
 *
 * Example:
 * ```kotlin
 *  assertThat(actualListOfDoubles).isContainedBy(
 *      expectedListOfDoubles,
 *      { a, b -> abs(a - b) < 0.07 },
 *      { a, b -> String.format("%.5f", b?.minus(a)?.absoluteValue ?: a).padStart(10, ' ') }
 *  )
 * ```
 */
fun <T> Assert<List<T>>.isContainedBy(
        expected: List<T>,
        isEqual: (T, T) -> Boolean,
        debugToString: ((T, T?) -> String)?
) = given { actual ->
    assertThat(actual, "Actual").size().isLessThanOrEqualTo(expected.size)
    val ei = expected.iterator()
    var ai = actual.iterator()
    var isContained = false
    val strings = arrayListOf("", "")
    val columnLengths = arrayListOf<Int>()
    var column = 0
    var row = 1
    while (ei.hasNext()) {
        val e = ei.next()
        if (!ai.hasNext()) {
            // reached the end of the seeking sequence
            isContained = true
            break
        }
        val col = debugToString?.invoke(e, null)?.padStart(5, ' ') ?: ""
        val columnLength = col.length
        strings[0] += col

        val a = ai.next()
        strings[row] += debugToString?.invoke(e, a)
                ?.padStart(5, ' ')
                ?.take(columnLength)
                ?: ""

        if (!isEqual(a, e)) {
            ai = actual.iterator()
            row++
            strings += columnLengths.joinToString(separator = "") { " ".repeat(it) }
        }
        columnLengths += columnLength
        column++
    }
    if (!ai.hasNext()) {
        // reached the end of the seeking sequence
        isContained = true
    }
    if (debugToString != null) println("Check content matrix:\n" + strings.joinToString("\n"))
    assertThat(isContained, "the array $actual (size=${actual.size}) to be contained in the $expected (size=${expected.size})").isTrue()
}

fun Assert<SampleVector>.isEqualTo(expected: SampleVector, precision: Double = 1e-12) = given { actual ->
    actual.forEachIndexed { index, item ->
        assertThat(item, name = "${name ?: ""}${show(index, "[]")}").isCloseTo(expected[index], precision)
    }
}