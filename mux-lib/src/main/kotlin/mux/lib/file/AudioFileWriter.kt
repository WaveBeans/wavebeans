package mux.lib.file

import mux.lib.stream.SampleStream
import java.io.OutputStream


abstract class AudioFileWriter<A>(
        protected val destination: OutputStream
) {
    abstract fun write(sampleStream: SampleStream)
}

class AudioFileWriterException(message: String, cause: Exception?) : Exception(message, cause) {
    constructor(message: String) : this(message, null)
}