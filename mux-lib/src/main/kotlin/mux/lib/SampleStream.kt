package mux.lib

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.lang.Math.*

abstract class SampleStream(
        val descriptor: AudioFileDescriptor
) {
    abstract fun toByteArray(): ByteArray

    abstract fun getInputStream(): InputStream

    abstract fun rangeProjection(sampleStartIdx: Int, sampleEndIdx: Int): SampleStream

    /** number of bytes in the stream. */
    abstract fun dataSize(): Int

    /** number of samples in the stream. */
    fun samplesCount(): Int = dataSize() / (descriptor.bitDepth / 8)

    /** length of the stream in milliseconds. */
    fun length(): Int = (samplesCount() / (descriptor.sampleRate.toFloat() / 1000.0f)).toInt()
}

class SampleByteArrayStream(private val buffer: ByteArray, descriptor: AudioFileDescriptor) : SampleStream(descriptor) {

    override fun dataSize(): Int = buffer.size

    override fun rangeProjection(sampleStartIdx: Int, sampleEndIdx: Int): SampleStream {
        val s = max(sampleStartIdx * descriptor.bitDepth / 8, 0)
        val e = min(sampleEndIdx * descriptor.bitDepth / 8, buffer.size)

        return SampleByteArrayStream(buffer.copyOfRange(s, e), descriptor)
    }

    override fun getInputStream(): InputStream = ByteArrayInputStream(buffer)

    override fun toByteArray(): ByteArray = buffer.copyOf()

}

