package mux.lib.io

import mux.lib.samplesCountToLength
import mux.lib.stream.SampleStream
import java.io.InputStream
import java.net.URI
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

fun SampleStream.toCsv(uri: String, timeUnit: TimeUnit = TimeUnit.MILLISECONDS, encoding: Charset = Charset.forName("UTF-8")): StreamOutput {
    return CsvSampleStreamOutput(URI(uri), timeUnit, this, encoding)
}

fun TimeUnit.abbreviation(): String {
    return when(this) {
        TimeUnit.NANOSECONDS -> "ns"
        TimeUnit.MICROSECONDS -> "us"
        TimeUnit.MILLISECONDS -> "ms"
        TimeUnit.SECONDS -> "s"
        TimeUnit.MINUTES -> "m"
        TimeUnit.HOURS -> "h"
        TimeUnit.DAYS -> "d"
    }
}

class CsvSampleStreamOutput(
        uri: URI,
        val outputTimeUnit: TimeUnit,
        stream: SampleStream,
        val encoding: Charset = Charset.forName("UTF-8")
) : FileStreamOutput<SampleStream>(stream, uri) {

    override fun header(dataSize: Int): ByteArray? = "time ${outputTimeUnit.abbreviation()}, value\n".toByteArray(encoding)

    override fun footer(dataSize: Int): ByteArray? = null

    override fun inputStream(sampleRate: Float, start: Long?, end: Long?, timeUnit: TimeUnit): InputStream {
        return object : InputStream() {

            private val iterator =
                    (if (start != null || end != null) stream.rangeProjection(start ?: 0, end, timeUnit) else stream)
                            .asSequence(sampleRate).iterator()
            private var buffer: ByteArray? = null
            private var row: Int = 0
            private var bufferPointer: Int = 0

            override fun read(): Int {
                if (buffer == null || bufferPointer >= buffer!!.size) {

                    if (!iterator.hasNext()) return -1

                    val sample = iterator.next()

                    val time = (start?.let { outputTimeUnit.convert(it, timeUnit) } ?: 0) +
                            samplesCountToLength(row.toLong(), sampleRate, outputTimeUnit)

                    val b = "$time,$sample\n"

                    buffer = b.toByteArray(encoding)
                    bufferPointer = 0
                    row++
                }

                return buffer!![bufferPointer++].toInt() and 0xFF
            }
        }
    }

}