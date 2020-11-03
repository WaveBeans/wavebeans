package io.wavebeans.tests

import assertk.Assert
import assertk.all
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import assertk.assertions.support.show

fun <T> Assert<Iterable<T>>.eachIndexed(expectedSize: Int? = null, f: (Assert<T>, Int) -> Unit) = given { actual ->
    all {
        expectedSize?.let { assertThat(actual.count(), "elements count").isEqualTo(it) }
        actual.forEachIndexed { index, item ->
            f(assertThat(item, name = "${name ?: ""}${show(index, "[]")}"), index)
        }
    }
}

fun <T> Assert<List<T>>.isContainedBy(expected: List<T>, isEqual: (T, T) -> Boolean = { a, b -> a == b }) = given { actual ->
    val ei = expected.iterator()
    var ai = actual.iterator()
    var isContained = false
    while (ei.hasNext()) {
        val e = ei.next()
        if (!ai.hasNext()) {
            isContained = true
            break
        }
        val a = ai.next()
        if (!isEqual(a, e)) {
            ai = actual.iterator()
        }
    }
    if (!ai.hasNext()) {
        isContained = true
    }
    assertThat(isContained, "the array $actual (size=${actual.size}) to be contained in the $expected (size=${expected.size})").isTrue()
}
