package mux.lib.io

import mux.lib.MuxStream
import mux.lib.timeToSampleIndexFloor
import java.io.*
import java.net.URI
import java.util.concurrent.TimeUnit

abstract class FileStreamOutput<S, T : MuxStream<S, *>>(
        val stream: T,
        val uri: URI
) : StreamOutput {

    private var writtenBytes: Int = 0
    private val tmpFile = File.createTempFile("mux", ".tmp")

    override fun writer(sampleRate: Float): Writer {

        return object : Writer {

            val fileOutputStream = FileOutputStream(tmpFile)
            val sampleIterator = stream.asSequence(sampleRate).iterator()
            var offset = 0L

            override fun write(duration: Long, timeUnit: TimeUnit): Boolean {
                val samplesCount = timeToSampleIndexFloor(duration, timeUnit, sampleRate).toInt()

                val samples = (0..samplesCount).asSequence()
                        .filter { sampleIterator.hasNext() }
                        .map { sampleIterator.next() }
                        .toList()

                if (samples.isNotEmpty()) {
                    val bytes = serialize(offset, sampleRate, samples)
                    val r = bytes.size
                    fileOutputStream.write(bytes, 0, r)
                    writtenBytes += r
                    offset += samples.count()
                }

                return samples.isNotEmpty()
            }

            override fun close() {
                fileOutputStream.close()
            }

        }
    }

    protected abstract fun header(dataSize: Int): ByteArray?

    protected abstract fun footer(dataSize: Int): ByteArray?

    protected abstract fun inputStream(sampleRate: Float): InputStream

    protected abstract fun serialize(offset: Long, sampleRate: Float, samples: List<S>): ByteArray

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