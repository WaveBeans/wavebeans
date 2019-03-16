package mux.lib

import java.io.InputStream

abstract class AudioFileReader<A : AudioFileDescriptor>(
        protected val source: InputStream
) {

    /**
     * Reads data from source, decode it and streams all decoded samples. Fully reads provided source.
     *
     * @return meta information about audio file
     */
    abstract fun read(): Pair<A, SampleStream>

}

class AudioFileReaderException(message: String, cause: Exception?) : Exception(message, cause) {
    constructor(message: String) : this(message, null)
}