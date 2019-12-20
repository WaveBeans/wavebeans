package io.wavebeans.lib.io

import io.wavebeans.lib.*
import io.wavebeans.lib.stream.fft.FftSample
import kotlinx.serialization.Serializable
import java.net.URI
import java.nio.charset.Charset

fun BeanStream<FftSample>.magnitudeToCsv(
        uri: String
): StreamOutput<FftSample> {
    return CsvFftStreamOutput(this, CsvFftStreamOutputParams(uri, true))
}

fun BeanStream<FftSample>.phaseToCsv(
        uri: String
): StreamOutput<FftSample> {
    return CsvFftStreamOutput(this, CsvFftStreamOutputParams(uri, false))
}

@Serializable
data class CsvFftStreamOutputParams(
        val uri: String,
        val isMagnitude: Boolean,
        val encoding: String = "UTF-8"
) : BeanParams()

class CsvFftStreamOutput(
        val stream: BeanStream<FftSample>,
        val params: CsvFftStreamOutputParams
) : StreamOutput<FftSample>, SinkBean<FftSample>, SinglePartitionBean {

    override fun writer(sampleRate: Float): Writer {
        var offset = 0L
        return object : FileWriter<FftSample>(URI(params.uri), stream, sampleRate) {
            override fun header(): ByteArray? = null

            override fun footer(): ByteArray? = null

            override fun serialize(element: FftSample): ByteArray {
                val seq = if (params.isMagnitude)
                    element.magnitude().map { it.toString() }
                else
                    element.phase().map { it.toString() }


                var b = (sequenceOf(element.time / 1e+6) + seq)
                        .joinToString(",")

                if (offset++ == 0L) {
                    b = (sequenceOf("time ms \\ freq hz") + element.frequency().map { it.toString() }).joinToString(",") +
                            "\n$b"
                }

                return (b + "\n").toByteArray(Charset.forName(params.encoding))
            }

        }
    }

    override val input: Bean<FftSample>
        get() = stream

    override val parameters: BeanParams = params
}