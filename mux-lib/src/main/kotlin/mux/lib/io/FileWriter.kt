package mux.lib.io

import mux.lib.BeanStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URI

abstract class FileWriter<T : Any, S : BeanStream<T, S>>(
        val uri: URI,
        val stream: S,
        val sampleRate: Float
) : Writer {

    protected abstract fun header(dataSize: Int): ByteArray?

    protected abstract fun footer(dataSize: Int): ByteArray?

    private val tmpFile = File.createTempFile("file-stream-output", ".tmp")
            .also { it.deleteOnExit() }

    private val file = FileOutputStream(tmpFile)
    val bufferSize = 512 * 1024
    private val buffer = BufferedOutputStream(file, bufferSize)

    val sampleIterator = stream.asSequence(sampleRate).iterator()

    private var writtenBytes: Int = 0

    override fun write(): Boolean {
        return if (sampleIterator.hasNext()) {
            val bytes = serialize(sampleRate, sampleIterator.next())
            val r = bytes.size
            buffer.write(bytes, 0, r)
            writtenBytes += r
            true
        } else {
            false
        }
    }

    protected abstract fun serialize(sampleRate: Float, element: T): ByteArray


    override fun close() {
        buffer.flush()
        file.close()

        FileOutputStream(File(uri)).use { f ->
            val header = header(writtenBytes)
            if (header != null) f.write(header)

            FileInputStream(tmpFile).use { tmpF ->
                val buf = ByteArray(bufferSize)
                do {
                    val r = tmpF.read(buf)
                    if (r != -1) f.write(buf, 0, r)
                } while (r != -1)
            }

            val footer = footer(writtenBytes)
            if (footer != null) f.write(footer)
        }
    }

}
