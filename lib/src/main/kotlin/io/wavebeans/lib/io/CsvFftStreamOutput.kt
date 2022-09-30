package io.wavebeans.lib.io

import io.wavebeans.lib.*
import io.wavebeans.lib.stream.fft.FftSample
import kotlinx.serialization.Serializable
import java.net.URI
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

fun BeanStream<FftSample>.magnitudeToCsv(
        uri: String,
        timeUnit: TimeUnit = TimeUnit.MILLISECONDS,
        encoding: String = "UTF-8"
): StreamOutput<FftSample> {
    return CsvFftStreamOutput(this, CsvFftStreamOutputParams(uri, timeUnit, true, encoding))
}

fun BeanStream<FftSample>.phaseToCsv(
        uri: String,
        timeUnit: TimeUnit = TimeUnit.MILLISECONDS,
        encoding: String = "UTF-8"
): StreamOutput<FftSample> {
    return CsvFftStreamOutput(this, CsvFftStreamOutputParams(uri, timeUnit, false, encoding))
}

@Serializable
data class CsvFftStreamOutputParams(
        val uri: String,
        val timeUnit: TimeUnit,
        val isMagnitude: Boolean,
        val encoding: String = "UTF-8"
) : BeanParams

class CsvFftStreamOutput(
        override val input: BeanStream<FftSample>,
        override val parameters: CsvFftStreamOutputParams
) : AbstractStreamOutput<FftSample>(input), SinkBean<FftSample>, SinglePartitionBean {

    override fun outputWriter(inputSequence: Sequence<FftSample>, sampleRate: Float): Writer {
        var offset = 0L
        val writer = FileWriterDelegate<Unit>({ URI(parameters.uri) })
        return object : AbstractWriter<FftSample>(
                input,
                sampleRate,
                writer,
                CsvFftStreamOutput::class
        ) {

            override fun header(): ByteArray? = null

            override fun footer(): ByteArray? = null

            override fun serialize(element: FftSample): ByteArray {
                fun format5(v: Double): String = String.format("%.5f", v)
                fun format10(v: Double): String = String.format("%.10f", v)
                val seq = if (parameters.isMagnitude)
                    element.magnitude().map(::format10)
                else
                    element.phase().map(::format10)

                val timeMarker = parameters.timeUnit.convert(element.time(), TimeUnit.NANOSECONDS)
                var b = (sequenceOf(timeMarker) + seq)
                        .joinToString(",")

                if (offset++ == 0L) {
                    b = (sequenceOf("time ms \\ freq hz") + element.frequency().map(::format5))
                            .joinToString(",") + "\n$b"
                }

                return (b + "\n").toByteArray(Charset.forName(parameters.encoding))
            }

        }
    }
}