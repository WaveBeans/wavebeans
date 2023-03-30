package io.wavebeans.lib.io

import io.wavebeans.lib.*
import io.wavebeans.lib.stream.fft.FftSample
import kotlinx.serialization.Serializable

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
                val seq = if (parameters.isMagnitude)
                    element.magnitude().map(Double::toString)
                else
                    element.phase().map(Double::toString)

                val timeMarker = parameters.timeUnit.convert(element.time(), TimeUnit.NANOSECONDS)
                var b = (sequenceOf(timeMarker) + seq)
                    .joinToString(",")

                if (offset++ == 0L) {
                    b = (sequenceOf("time ms \\ freq hz") + element.frequency().map(Double::toString))
                        .joinToString(",") + "\n$b"
                }

                return (b + "\n").encodeToByteArray()
            }

        }
    }
}