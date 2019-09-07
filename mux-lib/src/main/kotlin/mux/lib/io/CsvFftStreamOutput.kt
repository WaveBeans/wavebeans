package mux.lib.io

import mux.lib.Mux
import mux.lib.MuxNode
import mux.lib.MuxSingleInputNode
import mux.lib.samplesCountToLength
import mux.lib.stream.FftSample
import mux.lib.stream.FiniteFftStream
import java.io.InputStream
import java.net.URI
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

fun FiniteFftStream.magnitudeToCsv(uri: String, encoding: Charset = Charset.forName("UTF-8")): StreamOutput {
    return CsvFftStreamOutput(URI(uri), this, true, encoding)
}

fun FiniteFftStream.phaseToCsv(uri: String, encoding: Charset = Charset.forName("UTF-8")): StreamOutput {
    return CsvFftStreamOutput(URI(uri), this, false, encoding)
}

class CsvFftStreamOutput(
        uri: URI,
        stream: FiniteFftStream,
        val isMagnitude: Boolean,
        val encoding: Charset = Charset.forName("UTF-8")
) : FileStreamOutput<FftSample, FiniteFftStream>(stream, uri) {
    override fun mux(): MuxNode = MuxSingleInputNode(Mux("CsvFftStreamOutput(uri=$uri)"), stream.mux())

    override fun header(dataSize: Int): ByteArray? = null

    override fun footer(dataSize: Int): ByteArray? = null

    override fun inputStream(sampleRate: Float): InputStream {
        return object : InputStream() {

            private val iterator = (stream).asSequence(sampleRate).iterator()
            private var buffer: ByteArray? = null
            private var row: Int = 0
            private var bufferPointer: Int = 0

            override fun read(): Int {
                if (buffer == null || bufferPointer >= buffer!!.size) {

                    if (!iterator.hasNext()) return -1

                    val fftSample = iterator.next()

                    val seq = if (isMagnitude)
                        fftSample.magnitude.map { it.toString() }
                    else
                        fftSample.phase.map { it.toString() }

                    var b = (sequenceOf(fftSample.time / 10e+6) + seq)
                            .joinToString(",") + "\n"

                    if (row == 0) {
                        b = (sequenceOf("time ms \\ freq hz") + fftSample.frequency.map { it.toString() }).joinToString(",") +
                                "\n$b"
                    }
                    buffer = b.toByteArray(encoding)
                    bufferPointer = 0
                    row++
                }

                return buffer!![bufferPointer++].toInt() and 0xFF
            }
        }
    }

    override fun serialize(offset: Long, sampleRate: Float, samples: List<FftSample>): ByteArray {
        return samples.asSequence()
                .mapIndexed { idx, fftSample ->
                    val seq = if (isMagnitude)
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
                .toByteArray(encoding)
    }



}