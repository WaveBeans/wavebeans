package io.wavebeans.lib.io

import io.wavebeans.lib.Bean
import io.wavebeans.lib.BeanParams
import io.wavebeans.lib.stream.FftSample
import io.wavebeans.lib.stream.FiniteFftStream
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
        stream: FiniteFftStream,
        val params: CsvFftStreamOutputParams
) : StreamOutput<FftSample, FiniteFftStream> {

    override fun writer(sampleRate: Float): Writer {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override val input: Bean<FftSample, FiniteFftStream>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override val parameters: BeanParams = params

//    override fun header(dataSize: Int): ByteArray? = null
//
//    override fun footer(dataSize: Int): ByteArray? = null
//
//    override fun serialize(offset: Long, sampleRate: Float, samples: List<FftSample>): ByteArray {
//        return samples.asSequence()
//                .mapIndexed { idx, fftSample ->
//                    val seq = if (params.isMagnitude)
//                        fftSample.magnitude.map { it.toString() }
//                    else
//                        fftSample.phase.map { it.toString() }
//
//
//                    var b = (sequenceOf(fftSample.time / 10e+6) + seq)
//                            .joinToString(",")
//
//                    if (offset + idx == 0L) {
//                        b = (sequenceOf("time ms \\ freq hz") + fftSample.frequency.map { it.toString() }).joinToString(",") +
//                                "\n$b"
//                    }
//
//                    b
//                }
//                .joinToString(separator = "\n")
//                .toByteArray(params.encoding)
//    }



}