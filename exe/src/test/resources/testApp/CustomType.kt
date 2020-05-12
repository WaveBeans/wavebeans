package io.wavebeans.test.app

import io.wavebeans.lib.io.input
import io.wavebeans.lib.io.toCsv
import io.wavebeans.lib.stream.Measured
import io.wavebeans.lib.stream.map
import io.wavebeans.lib.stream.trim
import java.io.File

data class MeasuredSample(val value: Long) : Measured {
    override fun measure(): Int = 1
}

fun main() {
    val file = /*FILE*/ File.createTempFile("testAppOutput", ".csv").also { it.deleteOnExit() } /*FILE*/
    val o = input { (idx, _) -> if (idx < 10) idx else null }
            .map { MeasuredSample(it) }
            .trim(100)
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