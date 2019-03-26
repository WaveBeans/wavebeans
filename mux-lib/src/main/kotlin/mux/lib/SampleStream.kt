package mux.lib

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.lang.IllegalArgumentException
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

    fun downSample(ratio: Int): SampleStream = RatioDownSampledStream(this, ratio)
}

class RatioDownSampledStream(
        private val sourceStream: SampleStream,
        private val ratio: Int,
        sourceStartIdx: Int? = null,
        sourceEndIdx: Int? = null

) : SampleStream(sourceStream.descriptor.copy(sampleRate = sourceStream.descriptor.sampleRate / ratio)) {

    init {
        if (sourceStartIdx != null && sourceEndIdx ?: Int.MAX_VALUE <= sourceStartIdx)
            throw IllegalArgumentException("sourceStartIdx[$sourceStartIdx] should be less then sourceEndIdx[$sourceEndIdx]")
    }


    private val stream = if (sourceStartIdx != null) {
        sourceStream.rangeProjection(sourceStartIdx, sourceEndIdx ?: sourceStream.samplesCount())
    } else {
        sourceStream
    }

    private val bytesPerSample = sourceStream.descriptor.bitDepth / 8

    override fun toByteArray(): ByteArray {
        return stream.toByteArray()
                .filterIndexed { index, _ -> isAtRatio(index) }
                .toByteArray()
    }

    override fun getInputStream(): InputStream = SkipBytesInputStream(stream.getInputStream()) { !isAtRatio(it) }

    override fun rangeProjection(sampleStartIdx: Int, sampleEndIdx: Int): SampleStream {
        val sourceStartIdx = sampleStartIdx * ratio
        val sourceEndIdx = sampleEndIdx * ratio

        return RatioDownSampledStream(sourceStream, ratio, sourceStartIdx, sourceEndIdx)
    }

    override fun dataSize(): Int = stream.dataSize() / ratio

    private fun isAtRatio(byteIdx: Int) =
            (byteIdx / bytesPerSample) % ratio == 0

}

class SkipBytesInputStream(private val source: InputStream, val skip: (Int) -> Boolean) : InputStream() {

    private var index = 0

    override fun read(): Int {
        var r = source.read()
        if (r == -1) return -1
        do {
            if (skip(index)) {
                index++
                r = source.read()
            } else {
                break
            }
        } while (r != -1)
        index++
        return r
    }

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
