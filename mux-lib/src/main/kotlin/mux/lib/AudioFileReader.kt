package mux.lib

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.InputStream
import java.io.OutputStream

abstract class AudioFileReader<A : AudioFileDescriptor>(
        private val source: InputStream
) {

    protected abstract fun readHeader(source: InputStream): A

    protected abstract fun readSamples(descriptor: A, source: InputStream): SampleStream

    /**
     * Reads data from source, decode it and streams all decoded samples. Fully reads provided source.
     *
     * @return meta information about audio file
     */
    fun read(): Pair<A, SampleStream> {
        val s = BufferedInputStream(source)
        val descriptor = readHeader(s)

        val sampleStream = readSamples(descriptor, s)

        return Pair(descriptor, sampleStream)
    }

}

class AudioFileReaderException(message: String, cause: Exception?) : Exception(message, cause) {
    constructor(message: String) : this(message, null)
}