package io.wavebeans.lib.io

import io.wavebeans.lib.BeanStream
import mu.KotlinLogging
import java.io.*
import java.lang.Exception
import java.net.URI

abstract class FileWriter<T : Any>(
        val uri: URI,
        val stream: BeanStream<T>,
        val sampleRate: Float
) : Writer {

    companion object {
        private val log = KotlinLogging.logger { }
    }

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
            log.debug { "[$this uri=$uri] The stream is over" }
            false
        }
    }

    protected abstract fun serialize(element: T): ByteArray

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
                val header = header()
                if (header != null) f.write(header)

                FileInputStream(tmpFile).use { tmpF ->
                    val buf = ByteArray(bufferSize)
                    do {
                        val r = tmpF.read(buf)
                        if (r != -1) f.write(buf, 0, r)
                        log.trace { "[$this] Copied other $bufferSize bytes from $tmpFile to file with uri=$uri" }
                    } while (r != -1)
                }

                val footer = footer()
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
