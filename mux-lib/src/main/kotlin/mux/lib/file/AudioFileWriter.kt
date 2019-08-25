package mux.lib.file

import java.io.OutputStream


interface AudioFileWriter<A> {

    val destination: OutputStream

    fun write()
}

class AudioFileWriterException(message: String, cause: Exception?) : Exception(message, cause) {
    constructor(message: String) : this(message, null)
}