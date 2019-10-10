package mux.lib.io

import kotlinx.serialization.Serializable
import mux.lib.*
import mux.lib.stream.FiniteSampleStream
import java.net.URI
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

fun FiniteSampleStream.toCsv(
        uri: String,
        timeUnit: TimeUnit = TimeUnit.MILLISECONDS,
        encoding: String = "UTF-8"
): StreamOutput<SampleArray, FiniteSampleStream> {
    return CsvSampleStreamOutput(this, CsvSampleStreamOutputParams(uri, timeUnit, encoding))
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

@Serializable
data class CsvSampleStreamOutputParams(
        val uri: String,
        val outputTimeUnit: TimeUnit,
        val encoding: String = "UTF-8"
) : BeanParams() {

    // TODO come up with serializer
    fun uri(): URI = URI(uri)

    fun encoding(): Charset = Charset.forName(encoding)
}

class CsvSampleStreamOutput(
        val stream: FiniteSampleStream,
        val params: CsvSampleStreamOutputParams
) : StreamOutput<SampleArray, FiniteSampleStream> {

    override fun writer(sampleRate: Float): Writer {
        var offset = 0L
        return object : FileWriter<SampleArray, FiniteSampleStream>(URI(params.uri), stream, sampleRate) {
            override fun header(dataSize: Int): ByteArray? = "time ${params.outputTimeUnit.abbreviation()}, value\n".toByteArray(params.encoding())

            override fun footer(dataSize: Int): ByteArray? = null

            override fun serialize(sampleRate: Float, samples: SampleArray): ByteArray {
                return samples.asSequence()
                        .mapIndexed { idx, sample ->
                            val time = samplesCountToLength(offset++, sampleRate, params.outputTimeUnit)
                            "$time,$sample\n"
                        }
                        .joinToString(separator = "")
                        .toByteArray(params.encoding())
            }

        }
    }

    override val input: Bean<SampleArray, FiniteSampleStream>
        get() = stream

    override val parameters: BeanParams = params

}