package io.wavebeans.lib.io

import io.wavebeans.lib.BeanStream
import mu.KotlinLogging
import java.io.*
import java.net.URI

class FileBufferedWriter(
        val uri: URI
) : BufferedWriter() {

    companion object {
        private val log = KotlinLogging.logger { }
    }

    private val tmpFile = File.createTempFile("file-stream-output", ".tmp")

    private val file = FileOutputStream(tmpFile)
    private val bufferSize = 512 * 1024
    private val buffer = BufferedOutputStream(file, bufferSize)

    override fun write(b: Int) {
        buffer.write(b)
    }

    override fun close() {
        log.info { "[$this] Closing the writer" }
        try {
            buffer.close()
            log.debug { "[$this] Buffer closed" }
        } catch (e: IOException) {
            // ignore if buffer was already closed
            if ((e.message ?: "").contains("Closed"))
                log.warn(e) { "[$this] Buffer was already closed earlier" }
            else
                throw e
        }
        try {
            file.close()
            log.debug { "[$this] Temporary file $tmpFile closed" }
        } catch (e: IOException) {
            // ignore if buffer was already closed
            if ((e.message ?: "").contains("Closed"))
                log.warn(e) { "[$this] Temporary file $tmpFile was already closed earlier" }
            else
                throw e
        }

        if (tmpFile.exists()) {
            FileOutputStream(File(uri)).use { f ->
                val header = headerFn()
                if (header != null) f.write(header)

                FileInputStream(tmpFile).use { tmpF ->
                    val buf = ByteArray(bufferSize)
                    do {
                        val r = tmpF.read(buf)
                        if (r != -1) f.write(buf, 0, r)
                        log.trace { "[$this] Copied other $bufferSize bytes from $tmpFile to file with uri=$uri" }
                    } while (r != -1)
                }

                val footer = footerFn()
                if (footer != null) f.write(footer)
            }
            log.debug { "[$this] Temporary file $tmpFile copied over to file with uri=$uri" }
            tmpFile.delete()
            log.debug { "[$this] Temporary file $tmpFile deleted" }
        } else {
            log.warn { "[$this] Temporary file is not found. Seems everything is closed already." }
        }

    }

}

abstract class BufferedWriter(
) : OutputStream(), Closeable {

    var headerFn: () -> ByteArray? = { null }
        internal set

    var footerFn: () -> ByteArray? = { null }
        internal set
}

abstract class AbstractWriter<T : Any>(
        val stream: BeanStream<T>,
        val sampleRate: Float,
        val bufferedWriter: BufferedWriter
) : Writer {

    companion object {
        private val log = KotlinLogging.logger { }
    }

    init {
        bufferedWriter.headerFn = ::header
        bufferedWriter.footerFn = ::footer
    }

    protected abstract fun header(): ByteArray?

    protected abstract fun footer(): ByteArray?

    private val sampleIterator = stream.asSequence(sampleRate).iterator()

    override fun write(): Boolean {
        return if (sampleIterator.hasNext()) {
            val bytes = serialize(sampleIterator.next())
            bufferedWriter.write(bytes, 0, bytes.size)
            true
        } else {
            log.debug { "[$this] The stream is over" }
            false
        }
    }

    protected abstract fun serialize(element: T): ByteArray

    override fun close() {
        bufferedWriter.close()
    }

}
