package mux.lib.io

import mux.lib.stream.FftStream
import java.io.InputStream
import java.nio.charset.Charset

class CsvFftStreamOutput(
        val sampleRate: Float,
        val stream: FftStream,
        val isMagnitude: Boolean,
        val encoding: Charset = Charset.forName("UTF-8")
) : StreamOutput {

    override fun getInputStream(): InputStream {
        return object : InputStream() {

            private val iterator = stream.asSequence(sampleRate).iterator()
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

}