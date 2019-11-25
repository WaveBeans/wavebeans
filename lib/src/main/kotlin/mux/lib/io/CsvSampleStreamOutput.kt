package mux.lib.io

import kotlinx.serialization.Serializable
import mux.lib.*
import mux.lib.stream.FiniteSampleStream
import java.io.ByteArrayOutputStream
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
) : StreamOutput<SampleArray, FiniteSampleStream>, SinglePartitionBean {

    override fun writer(sampleRate: Float): Writer {
        var offset = 0L

        return object : FileWriter<SampleArray, FiniteSampleStream>(params.uri(), stream, sampleRate) {

            override fun header(): ByteArray? = "time ${params.outputTimeUnit.abbreviation()}, value\n".toByteArray(params.encoding())

            override fun footer(): ByteArray? = null

            override fun serialize(element: SampleArray): ByteArray {
                val baos = ByteArrayOutputStream(element.size * 20)
                for (i in 0 until element.size) {
                    if (offset % (sampleRate.toLong() * 1L) == 0L) {
                        println("Processed ${offset / sampleRate} seconds")
                    }
                    val sample = element[i]
                    val time = samplesCountToLength(offset++, sampleRate, params.outputTimeUnit)
                    baos.write(String.format("%d,%.10f\n", time, sample).toByteArray(params.encoding()))
                }
                return baos.toByteArray()
            }

        }
    }

    override val input: Bean<SampleArray, FiniteSampleStream>
        get() = stream

    override val parameters: BeanParams = params

}