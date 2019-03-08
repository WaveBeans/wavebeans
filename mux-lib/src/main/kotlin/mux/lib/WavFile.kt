package mux.lib

import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.InputStream
import java.io.OutputStream

/** RIFF constant which should always be first 4 bytes of the wav-file. */
private const val RIFF = 0x52494646
/** WAVE constant. */
private const val WAVE = 0x57415645
/** "fmt " constant */
private const val FMT = 0x666D7420
/** PCM format constant */
private const val PCM_FORMAT = 0x0100.toShort()
/** "data" constant */
private const val DATA = 0x64617461

/**
 * As by  http://soundfile.sapp.org/doc/WaveFormat/
 * Offset  Size  Name             Description
 *
 * The canonical WAVE format starts with the RIFF header:
 *
 * 0         4   ChunkID          Contains the letters "RIFF" in ASCII form
 *                                (0x52494646 big-endian form).
 * 4         4   ChunkSize        36 + SubChunk2Size, or more precisely:
 *                                4 + (8 + SubChunk1Size) + (8 + SubChunk2Size)
 *                                This is the size of the rest of the chunk
 *                                following this number.  This is the size of the
 *                                entire file in bytes minus 8 bytes for the
 *                                two fields not included in this count:
 *                                ChunkID and ChunkSize.
 * 8         4   Format           Contains the letters "WAVE"
 *                                (0x57415645 big-endian form).
 *
 * The "WAVE" format consists of two subchunks: "fmt " and "data":
 * The "fmt " subchunk describes the sound data's format:
 *
 * 12        4   Subchunk1ID      Contains the letters "fmt "
 *                                (0x666d7420 big-endian form).
 * 16        4   Subchunk1Size    16 for PCM.  This is the size of the
 *                                rest of the Subchunk which follows this number.
 * 20        2   AudioFormat      PCM = 1 (i.e. Linear quantization)
 *                                Values other than 1 indicate some
 *                                form of compression.
 * 22        2   NumChannels      Mono = 1, Stereo = 2, etc.
 * 24        4   SampleRate       8000, 44100, etc.
 * 28        4   ByteRate         == SampleRate * NumChannels * BitsPerSample/8
 * 32        2   BlockAlign       == NumChannels * BitsPerSample/8
 *                                The number of bytes for one sample including
 *                                all channels. I wonder what happens when
 *                                this number isn't an integer?
 * 34        2   BitsPerSample    8 bits = 8, 16 bits = 16, etc.
 *           2   ExtraParamSize   if PCM, then doesn't exist
 *           X   ExtraParams      space for extra parameters
 *
 * The "data" subchunk contains the size of the data and the actual sound:
 *
 * 36        4   Subchunk2ID      Contains the letters "data"
 *                                (0x64617461 big-endian form).
 * 40        4   Subchunk2Size    == NumSamples * NumChannels * BitsPerSample/8
 *                                This is the number of bytes in the data.
 *                                You can also think of this as the size
 *                                of the read of the subchunk following this
 *                                number.
 * 44        *   Data             The actual sound data.
 */

class WavFileReader(
        source: InputStream
) : AudioFileReader<WavLEAudioFileDescriptor>(source) {

    override fun readHeader(source: InputStream): WavLEAudioFileDescriptor {

        fun readByteArray(target: String, s: InputStream, amount: Int): ByteArray {
            val bb = ByteArray(amount)
            val r = s.read(bb)
            if (r == -1 || r != amount) {
                throw AudioFileReaderException("Can't read $target from the stream. Read $r bytes instead of $amount bytes")
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
                throw AudioFileReaderException("Can't read $target. Expected Int.", e)
            }
        }

        fun readLittleEndianShortAsInt(target: String, s: InputStream): Int {
            try {
                val bb = readByteArray(target, s, 2)
                return bb
                        .map { it.toInt() and 0xFF }
                        .foldIndexed(0x00) { i, a, v -> a or (v shl (i * 8)) }
            } catch (e: Exception) {
                throw AudioFileReaderException("Can't $target. Expected Short.", e)
            }
        }

        fun readConstantInt(target: String, s: DataInputStream, expected: Int): Int {
            val i = s.readInt()
            if (i != expected)
                throw AudioFileReaderException("was expecting '$target'=0x${expected.toString(16)} but found 0x${i.toString(16)}")
            return i
        }

        fun readConstantShort(target: String, s: DataInputStream, expected: Short): Int {
            val i = s.readShort()
            if (i != expected)
                throw AudioFileReaderException("was expecting '$target'=0x${expected.toString(16)} but found 0x${i.toString(16)}")
            return i.toInt()
        }

        fun startChunkStream(target: String, s: InputStream, size: Int): DataInputStream {
            try {
                val bb = readByteArray(target, s, size)
                return DataInputStream(ByteArrayInputStream(bb))
            } catch (e: Exception) {
                throw AudioFileReaderException("Can't read `$target` from the stream", e)
            }
        }

        val dis = DataInputStream(source)

        /** Chunk */
        readConstantInt("RIFF", dis, RIFF)
        /*val chunkSize = */readLittleEndianInt("chunkSize", dis)
        readConstantInt("WAVE", dis, WAVE)
        readConstantInt("fmt ", dis, FMT)

        /** sub Chunk 1 */
        val subChunkSize = readLittleEndianInt("subChunkSize", dis)
        val subChunk1Stream = startChunkStream("subChunk1", dis, subChunkSize)
        /*val audioFormat = */readConstantShort("PCM format", subChunk1Stream, PCM_FORMAT)
        val numberOfChannels = readLittleEndianShortAsInt("numberOfChannels", subChunk1Stream)
        val sampleRate = readLittleEndianInt("sampleRate", subChunk1Stream)
        /*val byteRate = */readLittleEndianInt("byteRate", subChunk1Stream)
        /*val blockAlign = */readLittleEndianShortAsInt("blockAlign", subChunk1Stream)
        val bitDepth = readLittleEndianShortAsInt("bitDepth", subChunk1Stream)

        /** sub Chunk 2 */
        /*val subChunk2ID =*/ readConstantInt("data", dis, DATA)
        val dataSize = readLittleEndianInt("dataSize", dis)

        return WavLEAudioFileDescriptor(sampleRate, bitDepth, numberOfChannels, dataSize)
    }

    override fun readSamples(descriptor: WavLEAudioFileDescriptor, source: InputStream): SampleStream {
        if (descriptor.numberOfChannels == 1) {
            if (descriptor.bitDepth == 16) {
                val bb = ByteArray(descriptor.dataSize)
                val c = source.read(bb)
                if (c != descriptor.dataSize) TODO("handle stream reading better")
                return SampleStream16(bb, descriptor)
            } else {
                TODO("make support of other bit depth")
            }
        } else {
            TODO("implement non-mono wav files")
        }
    }

}

class WavFileWriter(
        private val descriptor: AudioFileDescriptor,
        source: InputStream,
        destination: OutputStream,
        samplesCount: Int
) : AudioFileWriter<AudioFileDescriptor>(source, destination, samplesCount) {

    override fun writeBody(destination: OutputStream, sampleStream: InputStream) {
        repeat(dataSize) {
            destination.write(sampleStream.read())
        }
    }

    override fun getHeader(): ByteArray {
        TODO()
    }
}