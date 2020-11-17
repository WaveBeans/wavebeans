package io.wavebeans.lib.io

import io.wavebeans.fs.core.WbFileDriver
import io.wavebeans.lib.*
import io.wavebeans.lib.stream.FiniteToStream
import io.wavebeans.lib.stream.stream
import io.wavebeans.metrics.clazzTag
import io.wavebeans.metrics.samplesProcessedOnInputMetric
import kotlinx.serialization.Serializable
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.InputStream
import java.net.URI
import java.util.concurrent.TimeUnit
import kotlin.reflect.jvm.jvmName

fun wave(uri: String): FiniteInput<Sample> = WavFiniteInput(WavFiniteInputParams(uri))

fun wave(uri: String, converter: FiniteToStream<Sample>): BeanStream<Sample> = wave(uri).stream(converter)

@Serializable
data class WavFiniteInputParams(
        val uri: String
) : BeanParams()

class WavFiniteInput(
        val params: WavFiniteInputParams,
) : AbstractInputBeanStream<Sample>(), FiniteInput<Sample>, SinglePartitionBean {

    private val samplesProcessed = samplesProcessedOnInputMetric.withTags(clazzTag to WavFiniteInput::class.jvmName)

    override val parameters: BeanParams = params

    data class Content(
            val size: Int,
            val bitDepth: BitDepth,
            val buffer: ByteArray,
            val sampleRate: Float
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Content

            if (size != other.size) return false
            if (bitDepth != other.bitDepth) return false
            if (!buffer.contentEquals(other.buffer)) return false
            if (sampleRate != other.sampleRate) return false

            return true
        }

        override fun hashCode(): Int {
            var result = size
            result = 31 * result + bitDepth.hashCode()
            result = 31 * result + buffer.contentHashCode()
            result = 31 * result + sampleRate.hashCode()
            return result
        }
    }

    private val cnt: Content by lazy {
        val source = WbFileDriver.createFile(URI(params.uri)).createWbFileInputStream()
        val (descriptor, buf) = WavFileReader(source).read()
        Content(buf.size, descriptor.bitDepth, buf, descriptor.sampleRate)
    }


    override val desiredSampleRate: Float? by lazy { cnt.sampleRate }

    override fun length(timeUnit: TimeUnit): Long = samplesCountToLength(samplesCount().toLong(), cnt.sampleRate, timeUnit)

    override fun samplesCount(): Int = cnt.size / cnt.bitDepth.bytesPerSample

    override fun inputSequence(sampleRate: Float): Sequence<Sample> {
        require(sampleRate == cnt.sampleRate) { "The stream should be resampled from ${cnt.sampleRate}Hz to ${sampleRate}Hz" }
        return ByteArrayLittleEndianDecoder(cnt.sampleRate, cnt.bitDepth)
                .sequence(cnt.sampleRate, cnt.buffer)
                .map { samplesProcessed.increment(); it }
    }
}

class WavFileReaderException(message: String, cause: Exception?) : Exception(message, cause) {
    constructor(message: String) : this(message, null)
}

class WavFileReader(
        private val source: InputStream
) {

    fun read(): Pair<WavLEAudioFileDescriptor, ByteArray> {

        fun readByteArray(target: String, s: InputStream, amount: Int): ByteArray {
            val bb = ByteArray(amount)
            val r = s.read(bb)
            if (r == -1 || r != amount) {
                throw WavFileReaderException("Can't read $target from the stream. Read $r bytes instead of $amount bytes")
            }
            return bb
        }

        fun readLittleEndianInt(target: String, s: InputStream): Int {
            try {
                val bb = readByteArray(target, s, 4)
                return bb
                        .map { it.toInt() and 0xFF }
                        .foldIndexed(0x00) { i, a, v -> a or (v shl (i * 8)) }
            } catch (e: Exception) {
                throw WavFileReaderException("Can't read $target. Expected Int.", e)
            }
        }

        fun readLittleEndianShortAsInt(target: String, s: InputStream): Int {
            try {
                val bb = readByteArray(target, s, 2)
                return bb
                        .map { it.toInt() and 0xFF }
                        .foldIndexed(0x00) { i, a, v -> a or (v shl (i * 8)) }
            } catch (e: Exception) {
                throw WavFileReaderException("Can't $target. Expected Short.", e)
            }
        }

        fun readConstantInt(target: String, s: DataInputStream, expected: Int): Int {
            val i = s.readInt()
            if (i != expected)
                throw WavFileReaderException("was expecting '$target'=0x${expected.toString(16)} but found 0x${i.toString(16)}")
            return i
        }

        fun readConstantShort(target: String, s: DataInputStream, expected: Short): Int {
            val i = s.readShort()
            if (i != expected)
                throw WavFileReaderException("was expecting '$target'=0x${expected.toString(16)} but found 0x${i.toString(16)}")
            return i.toInt()
        }

        fun startChunkStream(target: String, s: InputStream, size: Int): DataInputStream {
            try {
                val bb = readByteArray(target, s, size)
                return DataInputStream(ByteArrayInputStream(bb))
            } catch (e: Exception) {
                throw WavFileReaderException("Can't read `$target` from the stream", e)
            }
        }

        val dis = DataInputStream(source)

        /** Chunk */
        readConstantInt("RIFF", dis, RIFF) // TODO handle RIFX -- Big Endian format
        /*val chunkSize = */readLittleEndianInt("chunkSize", dis)
        readConstantInt("WAVE", dis, WAVE)
        readConstantInt("fmt ", dis, FMT)

        /** sub Chunk 1 */
        val subChunk1Size = readLittleEndianInt("subChun1kSize", dis)
        val subChunk1Stream = startChunkStream("subChunk1", dis, subChunk1Size)
        /*val audioFormat = */readConstantShort("PCM format", subChunk1Stream, PCM_FORMAT)
        val numberOfChannels = readLittleEndianShortAsInt("numberOfChannels", subChunk1Stream)
        val sampleRate = readLittleEndianInt("sampleRate", subChunk1Stream)
        /*val byteRate = */readLittleEndianInt("byteRate", subChunk1Stream)
        /*val blockAlign = */readLittleEndianShortAsInt("blockAlign", subChunk1Stream)
        val bitDepth = readLittleEndianShortAsInt("bitDepth", subChunk1Stream)

        /** sub Chunk 2 */
        /*val subChunk2ID =*/ readConstantInt("data", dis, DATA)
        val dataSize = readLittleEndianInt("dataSize", dis)

        val bitDepthAsEnum = BitDepth.of(bitDepth)
        val sampleRateAsFloat = sampleRate.toFloat()
        val descriptor = WavLEAudioFileDescriptor(sampleRateAsFloat, bitDepthAsEnum, numberOfChannels)

        if (descriptor.numberOfChannels == 1) {
            val bb = ByteArray(dataSize)
            val c = source.read(bb)
            if (c != dataSize) throw IllegalStateException("Read $c bytes but expected $dataSize")
            return Pair(descriptor, bb)
        } else {
            throw UnsupportedOperationException("Mono wav-files are supported only.")
        }
    }

}