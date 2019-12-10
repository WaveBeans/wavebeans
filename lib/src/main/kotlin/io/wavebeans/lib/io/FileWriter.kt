package io.wavebeans.lib.io

import io.wavebeans.lib.BeanStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URI

abstract class FileWriter<T : Any>(
        val uri: URI,
        val stream: BeanStream<T>,
        val sampleRate: Float
) : Writer {

    protected abstract fun header(): ByteArray?

    protected abstract fun footer(): ByteArray?

    private val tmpFile = File.createTempFile("file-stream-output", ".tmp")

    private val file = FileOutputStream(tmpFile)
    private val bufferSize = 512 * 1024
    private val buffer = BufferedOutputStream(file, bufferSize)
    private val sampleIterator = stream.asSequence(sampleRate).iterator()

    override fun write(): Boolean {
        return if (sampleIterator.hasNext()) {
            val bytes = serialize(sampleIterator.next())
            buffer.write(bytes, 0, bytes.size)
            true
        } else {
            false
        }
    }

    protected abstract fun serialize(element: T): ByteArray

    override fun close() {
        buffer.close()
        file.close()

        FileOutputStream(File(uri)).use { f ->
            val header = header()
            if (header != null) f.write(header)

            FileInputStream(tmpFile).use { tmpF ->
                val buf = ByteArray(bufferSize)
                do {
                    val r = tmpF.read(buf)
                    if (r != -1) f.write(buf, 0, r)
                } while (r != -1)
            }

            val footer = footer()
            if (footer != null) f.write(footer)
        }

        tmpFile.delete()
    }

}
