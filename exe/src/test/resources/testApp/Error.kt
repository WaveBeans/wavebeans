package io.wavebeans.test.app

import io.wavebeans.lib.io.input
import io.wavebeans.lib.io.toDevNull
import io.wavebeans.lib.stream.map

fun main() {
    val o1 = input { (idx, _) -> if (idx < 10) idx else null }
            .map { throw IllegalStateException("output 1 doesn't work") }
            .toDevNull()
    val o2 = input { (idx, _) -> if (idx < 10) idx else null }
            .map { throw IllegalStateException("output 2 doesn't work") }
            .toDevNull()
    val outputs = listOf(o1, o2)

    run(outputs)
}