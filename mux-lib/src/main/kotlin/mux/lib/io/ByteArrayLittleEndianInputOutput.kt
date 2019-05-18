package mux.lib.io

import mux.lib.BitDepth
import mux.lib.stream.*
import java.io.BufferedInputStream
import java.io.InputStream

class ByteArrayLittleEndianAudioInput(val bitDepth: BitDepth, val buffer: ByteArray) : AudioInput {

    override fun info(): Map<String, String> {
        return mapOf(
                "Bit depth" to "${bitDepth.bits} bit",
                "Size" to "${buffer.size} bytes"
        )
    }

    override fun size(): Int = buffer.size / (bitDepth.bytesPerSample)

    override fun subInput(skip: Int, length: Int): AudioInput =
            ByteArrayLittleEndianAudioInput(bitDepth, buffer.copyOfRange(skip, skip + length))

    override fun asSequence(): Sequence<Sample> = buffer.iterator().asSequence()
            .windowed(bitDepth.bytesPerSample, bitDepth.bytesPerSample) { bytes ->
                when (bitDepth) {
                    BitDepth.BIT_8 -> sampleOf(bytes[0])
                    BitDepth.BIT_16 -> sampleOf(
                            bytes.foldIndexed(0) { index, acc, v ->
                                acc or (v.toInt() and 0xFF shl index * 8)
                            }.toShort()
                    )
                    BitDepth.BIT_24, BitDepth.BIT_32 -> sampleOf(
                            bytes.foldIndexed(0) { index, acc, v ->
                                acc or (v.toInt() and 0xFF shl index * 8)
                            }
                    )
                    BitDepth.BIT_64 -> sampleOf(
                            bytes.foldIndexed(0L) { index, acc, v ->
                                acc or (v.toLong() and 0xFF shl index * 8)
                            }
                    )
                }
            }
}

class ByteArrayLittleEndianAudioOutput(val bitDepth: BitDepth, val sampleStream: SampleStream) : AudioOutput {

    override fun toByteArray(): ByteArray {
        val buf = ByteArray(dataSize())
        BufferedInputStream(getInputStream()).use { s ->
            repeat(dataSize()) { i ->
                buf[i] = s.read().toByte()
            }
        }
        return buf
    }

    override fun getInputStream(): InputStream {
        return object : InputStream() {

            private val samplesToCache = 128 // TODO it doesn't seem required at all? However most likely depends on underlying sample stream.
            private val iterator = sampleStream.asSequence().iterator()
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
                            BitDepth.BIT_8 -> sample.asByte().toLong()
                            BitDepth.BIT_16 -> sample.asShort().toLong()
                            BitDepth.BIT_24, BitDepth.BIT_32 -> sample.asInt().toLong()
                            BitDepth.BIT_64 -> sample.asLong()
                        }

                        (0 until bitDepth.bytesPerSample)
                                .map { i -> ((bytesAsLong shr i * 8) and 0xFF).toByte() }
                                .forEachIndexed { i, v ->
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

    override fun dataSize(): Int =
            sampleStream.samplesCount() * bitDepth.bytesPerSample

}