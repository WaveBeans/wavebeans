package io.wavebeans.test.app

import io.wavebeans.lib.io.input
import io.wavebeans.lib.io.toCsv
import io.wavebeans.lib.stream.map
import java.io.File

data class MySample(val value: Long)

fun main() {
    val file = /*FILE*/ File.createTempFile("testAppOutput", ".csv").also { it.deleteOnExit() } /*FILE*/
    val o = input { (idx, _) -> if (idx < 10) idx else null }
            .map { MySample(it) }
            .toCsv(
                    uri = "file:///${file.absolutePath}",
                    header = listOf("index,value"),
                    elementSerializer = { (idx, _, sample) ->
                        listOf(idx.toString(), sample.value.toString())
                    }
            )
    val outputs = listOf(o)

    run(outputs)
}