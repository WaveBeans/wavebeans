package mux.lib.io

import mux.lib.MuxStream
import java.io.*
import java.net.URI
import java.util.concurrent.TimeUnit

abstract class FileStreamOutput<T : MuxStream<*, *>>(
        val stream: T,
        val uri: URI
) : StreamOutput {

    private val tmpFile = File.createTempFile("mux", ".tmp")!!
    private var writtenBytes: Int = 0

    override fun write(sampleRate: Float, start: Long?, end: Long?, timeUnit: TimeUnit): Boolean {
        var endOfStream = false
        FileOutputStream(tmpFile).use { f ->
            BufferedInputStream(inputStream(sampleRate, start, end, timeUnit)).use { i ->
                val bufferSize = 16384
                val buf = ByteArray(bufferSize)
                do {
                    val r = i.read(buf)
                    if (r != -1) {
                        f.write(buf, 0, r)
                        writtenBytes += r
                    } else {
                        endOfStream = true
                    }
                } while (r != -1)
            }
        }
        return endOfStream
    }

    protected abstract fun header(dataSize: Int): ByteArray?

    protected abstract fun footer(dataSize: Int): ByteArray?

    protected abstract fun inputStream(sampleRate: Float, start: Long?, end: Long?, timeUnit: TimeUnit): InputStream

    override fun close() {
        FileOutputStream(File(uri)).use { f ->
            val header = header(writtenBytes)
            if (header != null) f.write(header)

            FileInputStream(tmpFile).use { tmpF ->
                val buf = ByteArray(16384)
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