package mux.lib.io

import mux.lib.Mux
import mux.lib.MuxNode
import mux.lib.MuxSingleInputNode
import mux.lib.samplesCountToLength
import mux.lib.stream.FiniteSampleStream
import java.io.InputStream
import java.net.URI
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

fun FiniteSampleStream.toCsv(uri: String, timeUnit: TimeUnit = TimeUnit.MILLISECONDS, encoding: Charset = Charset.forName("UTF-8")): StreamOutput {
    return CsvSampleStreamOutput(URI(uri), timeUnit, this, encoding)
}

fun TimeUnit.abbreviation(): String {
    return when (this) {
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
        stream: FiniteSampleStream,
        val encoding: Charset = Charset.forName("UTF-8")
) : FileStreamOutput<FiniteSampleStream>(stream, uri) {

    override fun mux(): MuxNode = MuxSingleInputNode(Mux("CsvSampleStreamOutput(uri=$uri)"), stream.mux())

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