package mux.lib.io

import mux.lib.MuxParams
import mux.lib.stream.FftSample
import mux.lib.stream.FiniteFftStream
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
) : MuxParams

class CsvFftStreamOutput(
        stream: FiniteFftStream,
        val params: CsvFftStreamOutputParams
) : FileStreamOutput<FftSample, FiniteFftStream>(stream, params.uri) {

    override val parameters: MuxParams = params

    override fun header(dataSize: Int): ByteArray? = null

    override fun footer(dataSize: Int): ByteArray? = null

    override fun serialize(offset: Long, sampleRate: Float, samples: List<FftSample>): ByteArray {
        return samples.asSequence()
                .mapIndexed { idx, fftSample ->
                    val seq = if (params.isMagnitude)
                        fftSample.magnitude.map { it.toString() }
                    else
                        fftSample.phase.map { it.toString() }


                    var b = (sequenceOf(fftSample.time / 10e+6) + seq)
                            .joinToString(",")

                    if (offset + idx == 0L) {
                        b = (sequenceOf("time ms \\ freq hz") + fftSample.frequency.map { it.toString() }).joinToString(",") +
                                "\n$b"
                    }

                    b
                }
                .joinToString(separator = "\n")
                .toByteArray(params.encoding)
    }



}