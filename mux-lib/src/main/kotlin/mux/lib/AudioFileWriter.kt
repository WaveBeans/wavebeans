package mux.lib

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.InputStream
import java.io.OutputStream


abstract class AudioFileWriter<A>(
        private val sampleStream: InputStream,
        private val destination: OutputStream,
        protected val dataSize: Int
) {

    protected abstract fun getHeader(): ByteArray

    protected abstract fun writeBody(destination: OutputStream, sampleStream: InputStream)

    fun write() {
        val bufSize = 4096
        val d = BufferedOutputStream(destination, bufSize)
        val s = BufferedInputStream(sampleStream, bufSize)

        val header = getHeader()
        d.write(header)

        writeBody(d, s)
    }
}