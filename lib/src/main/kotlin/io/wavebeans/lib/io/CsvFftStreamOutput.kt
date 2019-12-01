package io.wavebeans.lib.io

import io.wavebeans.lib.Bean
import io.wavebeans.lib.BeanParams
import io.wavebeans.lib.stream.fft.FftSample
import io.wavebeans.lib.stream.fft.FiniteFftStream
import java.net.URI
import java.nio.charset.Charset

fun FiniteFftStream.magnitudeToCsv(
        uri: String,
        encoding: Charset = Charset.forName("UTF-8")
): StreamOutput<FftSample, FiniteFftStream> {
    return CsvFftStreamOutput(this, CsvFftStreamOutputParams(URI(uri), true, encoding))
}

fun FiniteFftStream.phaseToCsv(
        uri: String,
        encoding: Charset = Charset.forName("UTF-8")
): StreamOutput<FftSample, FiniteFftStream> {
    return CsvFftStreamOutput(this, CsvFftStreamOutputParams(URI(uri), false, encoding))
}

data class CsvFftStreamOutputParams(
        val uri: URI,
        val isMagnitude: Boolean,
        val encoding: Charset = Charset.forName("UTF-8")
) : BeanParams()

class CsvFftStreamOutput(
        val stream: FiniteFftStream,
        val params: CsvFftStreamOutputParams
) : StreamOutput<FftSample, FiniteFftStream> {

    override fun writer(sampleRate: Float): Writer {
        var offset = 0L
        return object : FileWriter<FftSample, FiniteFftStream>(params.uri, stream, sampleRate) {
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

                return (b + "\n").toByteArray(params.encoding)
            }

        }
    }

    override val input: Bean<FftSample, FiniteFftStream>
        get() = stream

    override val parameters: BeanParams = params
}