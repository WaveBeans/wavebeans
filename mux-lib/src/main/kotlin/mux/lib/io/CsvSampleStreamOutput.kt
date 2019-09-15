package mux.lib.io

import mux.lib.*
import mux.lib.stream.FiniteSampleStream
import java.net.URI
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

fun FiniteSampleStream.toCsv(
        uri: String,
        timeUnit: TimeUnit = TimeUnit.MILLISECONDS,
        encoding: Charset = Charset.forName("UTF-8")
): StreamOutput<Sample, FiniteSampleStream> {
    return CsvSampleStreamOutput(URI(uri), this, timeUnit, encoding)
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
        stream: FiniteSampleStream,
        val outputTimeUnit: TimeUnit,
        val encoding: Charset = Charset.forName("UTF-8")
) : FileStreamOutput<Sample, FiniteSampleStream>(stream, uri) {

    override fun header(dataSize: Int): ByteArray? = "time ${outputTimeUnit.abbreviation()}, value\n".toByteArray(encoding)

    override fun footer(dataSize: Int): ByteArray? = null

    override fun serialize(offset: Long, sampleRate: Float, samples: List<Sample>): ByteArray {
        return samples.asSequence()
                .mapIndexed { idx, sample ->
                    val time = samplesCountToLength(offset + idx, sampleRate, outputTimeUnit)
                    "$time,$sample\n"
                }
                .joinToString(separator = "")
                .toByteArray(encoding)
    }
}