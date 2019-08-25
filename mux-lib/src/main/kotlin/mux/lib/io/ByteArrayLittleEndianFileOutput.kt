package mux.lib.io

import mux.lib.*
import mux.lib.stream.*
import java.io.InputStream
import java.net.URI
import java.util.concurrent.TimeUnit

abstract class ByteArrayLittleEndianFileOutput(
        uri: URI,
        sampleStream: SampleStream,
        val bitDepth: BitDepth
) : FileStreamOutput<SampleStream>(sampleStream, uri) {

    private var sampleRate: Float? = null

    protected fun sampleRate(): Float = sampleRate ?: throw IllegalStateException("Sample rate is not yet initialized")

    override fun inputStream(sampleRate: Float, start: Long?, end: Long?, timeUnit: TimeUnit): InputStream {
        this.sampleRate = sampleRate

        return object : InputStream() {

            private val samplesToCache = 128 // TODO it doesn't seem required at all? However most likely depends on underlying sample stream.
            private val iterator =
                    (if (start != null || end != null) stream.rangeProjection(start ?: 0, end, timeUnit) else stream)
                            .asSequence(sampleRate).iterator()
            private val buf: ByteArray = ByteArray(bitDepth.bytesPerSample * samplesToCache)
            private var bufSize = 0
            private var bufIndex = Int.MAX_VALUE

            override fun read(): Int {
                return if (bufIndex >= bufSize && iterator.hasNext()) {
                    bufSize = 0
                    // buffer is empty and something left to read
                    var samplesRead = 0

                    while (iterator.hasNext() && samplesRead < samplesToCache) {
                        val sample = iterator.next()

                        val bytesAsLong = when (bitDepth) {
                            BitDepth.BIT_8 -> -sample.asByte().toLong()
                            BitDepth.BIT_16 -> sample.asShort().toLong()
                            BitDepth.BIT_24, BitDepth.BIT_32 -> sample.asInt().toLong()
                            BitDepth.BIT_64 -> sample.asLong()
                        }

                        (0 until bitDepth.bytesPerSample)
                                .map { i -> ((bytesAsLong shr i * 8) and 0xFF).toByte() }
                                .forEachIndexed { i: Int, v: Byte ->
                                    buf[samplesRead * bitDepth.bytesPerSample + i] = v
                                    bufSize++
                                }
                        samplesRead++
                    }
                    bufIndex = 1

                    buf[0].toInt() and 0xFF

                } else if (bufIndex < bufSize) {
                    // there is something in buffer -- return it
                    val r = buf[bufIndex]
                    bufIndex++

                    r.toInt() and 0xFF

                } else {
                    // nothing left to read, end of stream
                    -1
                }
            }

        }
    }
}