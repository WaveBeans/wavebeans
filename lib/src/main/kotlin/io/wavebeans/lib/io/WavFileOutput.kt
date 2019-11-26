package io.wavebeans.lib.io

import io.wavebeans.lib.*
import io.wavebeans.lib.stream.FiniteSampleStream
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.net.URI


fun FiniteSampleStream.toMono8bitWav(uri: String): StreamOutput<SampleArray, FiniteSampleStream> {
    return WavFileOutput(this, WavFileOutputParams(URI(uri), BitDepth.BIT_8, 1))
}

fun FiniteSampleStream.toMono16bitWav(uri: String): StreamOutput<SampleArray, FiniteSampleStream> {
    return WavFileOutput(this, WavFileOutputParams(URI(uri), BitDepth.BIT_16, 1))
}

fun FiniteSampleStream.toMono24bitWav(uri: String): StreamOutput<SampleArray, FiniteSampleStream> {
    return WavFileOutput(this, WavFileOutputParams(URI(uri), BitDepth.BIT_24, 1))
}

fun FiniteSampleStream.toMono32bitWav(uri: String): StreamOutput<SampleArray, FiniteSampleStream> {
    return WavFileOutput(this, WavFileOutputParams(URI(uri), BitDepth.BIT_32, 1))
}


class WavFileWriterException(message: String, cause: Exception?) : Exception(message, cause) {
    constructor(message: String) : this(message, null)
}

data class WavFileOutputParams(
        val uri: URI,
        val bitDepth: BitDepth,
        val numberOfChannels: Int
) : BeanParams()

class WavFileOutput(
        val stream: FiniteSampleStream,
        val params: WavFileOutputParams
) : StreamOutput<SampleArray, FiniteSampleStream>, SinglePartitionBean {

    override val input: Bean<SampleArray, FiniteSampleStream>
        get() = stream

    override val parameters: BeanParams = params

    override fun writer(sampleRate: Float): Writer {
        return object : ByteArrayLEFileOutputWriter(params.uri, stream, params.bitDepth, sampleRate) {

            override fun header(): ByteArray? {
                val destination = ByteArrayOutputStream()
                /** Create sub chunk 1 content*/
                val sc1Content = ByteArrayOutputStream()
                val sc1ContentStream = DataOutputStream(sc1Content)
                writeConstantShort("PCM audio format", sc1ContentStream, PCM_FORMAT)
                writeLittleEndianIntAsShort("numberOfChannels", sc1ContentStream, params.numberOfChannels)
                writeLittleEndianInt("sampleRate", sc1ContentStream, sampleRate.toInt())
                writeLittleEndianInt("byteRate", sc1ContentStream, sampleRate.toInt() * params.numberOfChannels * bitDepth.bytesPerSample)
                writeLittleEndianIntAsShort("byteAlign", sc1ContentStream, params.numberOfChannels * bitDepth.bytesPerSample)
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

            override fun footer(): ByteArray? = null

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
    }
}