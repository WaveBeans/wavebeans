package io.wavebeans.lib.io

import io.wavebeans.fs.core.WbFileDriver
import io.wavebeans.lib.*
import io.wavebeans.lib.stream.*
import io.wavebeans.metrics.clazzTag
import io.wavebeans.metrics.samplesProcessedOnInputMetric
import kotlinx.serialization.Serializable
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.InputStream
import java.net.URI
import java.util.concurrent.TimeUnit
import kotlin.reflect.jvm.jvmName

const val sincResampleFuncDefaultWindowSize = 64

/**
 * Reads the mono-wave file from [uri] resampling with [resampleFn] if required to match the output sample rate.
 *
 * @param uri the URI of the file of any [WbFileDriver], i.e. `file:///my/file.wav` for UNIX-like file systems,
 *            or `file://c:/my/file.wav` for Windows-like file systems.
 * @param resampleFn if the wav file sampled at a different sample rate, it'll be converted using that function, by
 *                   default [SincResampleFn] is used with [sincResampleFuncDefaultWindowSize]. Specify `null` to
 *                   avoid calling the resampling implicitly.
 * @return finite stream with the contents of the wave-file.
 */
fun wave(
        uri: String,
        resampleFn: Fn<ResamplingArgument<Sample>, Sequence<Sample>>? = sincResampleFunc(sincResampleFuncDefaultWindowSize),
): FiniteStream<Sample> = WavInput(WavInputParams(uri)).let { input ->
    resampleFn?.let { input.resample<FiniteStream<Sample>>(resampleFn = resampleFn) } ?: input
}

/**
 * Reads the mono-wave file from [uri] resampling with [resampleFn] if required to match the output sample rate.
 * The stream is converted to infinite one with the help of [converter].
 *
 * @param uri the URI of the file of any [WbFileDriver], i.e. `file:///my/file.wav` for UNIX-like file systems,
 *            or `file://c:/my/file.wav` for Windows-like file systems.
 * @param converter the function that converts finite stream to an infinite one.
 * @param resampleFn if the wav file sampled at a different sample rate, it'll be converted using that function, by
 *                   default [SincResampleFn] is used with [sincResampleFuncDefaultWindowSize]. Specify `null` to
 *                   avoid calling the resampling implicitly.
 * @return infinite stream with the contents of the wave-file.
 */
fun wave(
        uri: String,
        converter: FiniteToStream<Sample>,
        resampleFn: Fn<ResamplingArgument<Sample>, Sequence<Sample>> = sincResampleFunc(sincResampleFuncDefaultWindowSize),
): BeanStream<Sample> = wave(uri, resampleFn).stream(converter)

/**
 * Parameters for [WavInput]:
 * * [uri] - the URI of the file of any [WbFileDriver], i.e. `file:///my/file.wav` for UNIX-like file systems,
 *            or `file://c:/my/file.wav` for Windows-like file systems.
 */
@Serializable
data class WavInputParams(
        /**
         * The URI of the file of any [WbFileDriver], i.e. `file:///my/file.wav` for UNIX-like file systems,
         * or `file://c:/my/file.wav` for Windows-like file systems.
         */
        val uri: String,
) : BeanParams

/**
 * Reads the mono-wave file from [WavInputParams.uri].
 */
class WavInput(
        val params: WavInputParams,
) : AbstractInputBeanStream<Sample>(), FiniteStream<Sample>, SinglePartitionBean {

    private val samplesProcessed = samplesProcessedOnInputMetric.withTags(clazzTag to WavInput::class.jvmName)

    override val parameters: BeanParams = params

    data class Content(
            val size: Int,
            val bitDepth: BitDepth,
            val byteStream: InputStream,
            val sampleRate: Float,
    )

    private var cnt: Content? = null

    private fun readContent(): Content {
        val source = WbFileDriver.createFile(URI(params.uri)).createWbFileInputStream()
        val (descriptor, buf) = WavFileReader(source).read()
        val content = Content(descriptor.dataSize, descriptor.bitDepth, buf, descriptor.sampleRate)
        cnt = content
        return content
    }

    override val desiredSampleRate: Float? by lazy {
        val content = cnt ?: readContent()
        content.sampleRate
    }

    override fun length(timeUnit: TimeUnit): Long {
        val content = cnt ?: readContent()
        return samplesCountToLength(samplesCount(), content.sampleRate, timeUnit)
    }

    override fun samplesCount(): Long {
        val content = cnt ?: readContent()
        return (content.size / content.bitDepth.bytesPerSample).toLong()
    }

    override fun inputSequence(sampleRate: Float): Sequence<Sample> {
        val content = cnt ?: readContent()
        require(sampleRate == content.sampleRate) {
            "The stream should be resampled from ${content.sampleRate}Hz to ${sampleRate}Hz"
        }
        cnt = null
        return ByteArrayLittleEndianDecoder(content.bitDepth)
                .sequence(content.byteStream)
                .map { samplesProcessed.increment(); it }
    }
}

class WavFileReaderException(message: String, cause: Exception?) : Exception(message, cause) {
    constructor(message: String) : this(message, null)
}

class WavFileReader(
        private val source: InputStream,
) {

    fun read(): Pair<WavLEAudioFileDescriptor, InputStream> {

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
                throw WavFileReaderException("Can't read $target. Expected Short.", e)
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
        val descriptor = WavLEAudioFileDescriptor(sampleRateAsFloat, bitDepthAsEnum, numberOfChannels, dataSize)

        if (descriptor.numberOfChannels == 1) {
            return Pair(descriptor, source)
        } else {
            throw UnsupportedOperationException("Mono wav-files are supported only.")
        }
    }

}