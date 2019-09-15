package mux.lib.io

import mux.lib.*
import mux.lib.stream.FiniteSampleStream
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.net.URI


fun FiniteSampleStream.toMono8bitWav(uri: String): StreamOutput<Sample, FiniteSampleStream> {
    return WavFileOutput(URI(uri), this, BitDepth.BIT_8, 1)
}

fun FiniteSampleStream.toMono16bitWav(uri: String): StreamOutput<Sample, FiniteSampleStream> {
    return WavFileOutput(URI(uri), this, BitDepth.BIT_16, 1)
}

fun FiniteSampleStream.toMono24bitWav(uri: String): StreamOutput<Sample, FiniteSampleStream> {
    return WavFileOutput(URI(uri), this, BitDepth.BIT_24, 1)
}

fun FiniteSampleStream.toMono32bitWav(uri: String): StreamOutput<Sample, FiniteSampleStream> {
    return WavFileOutput(URI(uri), this, BitDepth.BIT_32, 1)
}


class WavFileWriterException(message: String, cause: Exception?) : Exception(message, cause) {
    constructor(message: String) : this(message, null)
}

class WavFileOutput(
        uri: URI,
        finiteSampleStream: FiniteSampleStream,
        bitDepth: BitDepth,
        private val numberOfChannels: Int
) : ByteArrayLittleEndianFileOutput(uri, finiteSampleStream, bitDepth) {

    override fun header(dataSize: Int): ByteArray? {
        val destination = ByteArrayOutputStream()
        /** Create sub chunk 1 content*/
        val sc1Content = ByteArrayOutputStream()
        val sc1ContentStream = DataOutputStream(sc1Content)
        writeConstantShort("PCM audio format", sc1ContentStream, PCM_FORMAT)
        writeLittleEndianIntAsShort("numberOfChannels", sc1ContentStream, numberOfChannels)
        writeLittleEndianInt("sampleRate", sc1ContentStream, sampleRate().toInt())
        writeLittleEndianInt("byteRate", sc1ContentStream, sampleRate().toInt() * numberOfChannels * bitDepth.bytesPerSample)
        writeLittleEndianIntAsShort("byteAlign", sc1ContentStream, numberOfChannels * bitDepth.bytesPerSample)
        writeLittleEndianIntAsShort("bitDepth", sc1ContentStream, bitDepth.bits)
        val sc1 = sc1Content.toByteArray()

        /** Creating sub chunk. */
        val subChunk1ByteArrayStream = ByteArrayOutputStream()
        val chunk1Stream = DataOutputStream(subChunk1ByteArrayStream)
        writeConstantInt("WAVE", chunk1Stream, WAVE)
        writeConstantInt("fmt", chunk1Stream, FMT)
        writeLittleEndianInt("subChunk1Size", chunk1Stream, sc1.size)
        chunk1Stream.write(sc1)
        val subChunk1 = subChunk1ByteArrayStream.toByteArray()

        val dos = DataOutputStream(destination)
        /** Chunk */
        writeConstantInt("RIFF", dos, RIFF) // TODO consider writing RIFX -- Big Endian format
        val chunkSize = 4 + (8 + subChunk1.size) + (8 + dataSize)
        writeLittleEndianInt("chunkSize", dos, chunkSize)
        /** Sub Chunk 1 */
        dos.write(subChunk1)

        /** Sub Chunk 2 */
        writeConstantInt("Data", dos, DATA)
        writeLittleEndianInt("dataSize", dos, dataSize)

        return destination.toByteArray()
    }

    override fun footer(dataSize: Int): ByteArray? = null

    protected fun writeConstantInt(target: String, d: DataOutputStream, v: Int) {
        try {
            d.writeInt(v)
        } catch (e: Exception) {
            throw WavFileWriterException("Can't write `$v` for `$target`.", e)
        }
    }

    protected fun writeConstantShort(target: String, d: DataOutputStream, v: Short) {
        try {
            d.writeShort(v.toInt())
        } catch (e: Exception) {
            throw WavFileWriterException("Can't write `$v` for `$target`.", e)
        }
    }

    protected fun writeLittleEndianInt(target: String, d: DataOutputStream, v: Int) {
        try {
            val b = (0..3)
                    .map { ((v shr (it * 8)) and 0xFF).toByte() }
                    .toByteArray()
            d.write(b)
        } catch (e: Exception) {
            throw WavFileWriterException("Can't write `$v` for $target.", e)
        }

    }

    protected fun writeLittleEndianIntAsShort(target: String, d: DataOutputStream, v: Int) {
        try {
            val b = (0..1)
                    .map { ((v shr (it * 8)) and 0xFF).toByte() }
                    .toByteArray()
            d.write(b)
        } catch (e: Exception) {
            throw WavFileWriterException("Can't write `$v` for $target", e)
        }

    }
}